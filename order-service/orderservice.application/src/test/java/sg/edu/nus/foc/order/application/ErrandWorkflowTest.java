package sg.edu.nus.foc.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.transaction.support.*;

import sg.edu.nus.foc.order.application.ErrandWorkflow.*;
import sg.edu.nus.foc.order.domain.*;

import java.time.*;
import java.util.*;

class ErrandWorkflowTest {
    ErrandRepository errands = mock(ErrandRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    CheckpointRepository checkpoints = mock(CheckpointRepository.class);
    CommandReceiptRepository commands = mock(CommandReceiptRepository.class);
    SupplierGateway suppliers = mock(SupplierGateway.class);
    CreditGateway credits = mock(CreditGateway.class);
    Instant now = Instant.parse("2026-09-29T12:00:00Z");
    ErrandWorkflow workflow =
            new ErrandWorkflow(
                    errands,
                    orders,
                    checkpoints,
                    commands,
                    suppliers,
                    credits,
                    Clock.fixed(now, ZoneOffset.UTC));
    CreateInput input =
            new CreateInput("cmd", "r", "Bring my lunch", "p", "d", 5, 30, now.plusSeconds(1800));

    @BeforeEach
    void setup() {
        TransactionSynchronizationManager.initSynchronization();
        when(credits.reserve(anyString(), anyString(), anyLong())).thenReturn(true);
        when(suppliers.resolve(anyString()))
                .thenAnswer(
                        invocation ->
                                Optional.of(
                                        new SupplierGateway.Supplier(
                                                invocation.getArgument(0), "name", "location")));
    }

    @AfterEach
    void clear() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void creationRetryValidationAndCompensation() {
        Errand created = workflow.create(input).getData().errand();
        assertThat(created.getStatus()).isEqualTo(Errand.Status.OPEN);
        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(credits, never()).release(anyString(), anyString());
        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(credits).release(created.getId(), "r");
        when(errands.findById(created.getId())).thenReturn(Optional.of(created));
        assertThat(workflow.create(input).getData().errand()).isSameAs(created);
        assertThat(
                        workflow.create(
                                        new CreateInput(
                                                "cmd",
                                                "r",
                                                "Changed description",
                                                "p",
                                                "d",
                                                5,
                                                30,
                                                input.expiresAt()))
                                .getError()
                                .getCode())
                .isEqualTo(OrderProblem.Code.CONFLICT);
        assertThat(
                        workflow.create(
                                        new CreateInput(
                                                "cmd2", "r", "Bad\ntext", "p", "p", 5, 30, now))
                                .getError()
                                .getDetails())
                .hasSize(3);
        when(credits.reserve(anyString(), anyString(), anyLong())).thenReturn(false);
        assertThat(
                        workflow.create(
                                        new CreateInput(
                                                "cmd3",
                                                "r",
                                                "Bring my lunch",
                                                "p",
                                                "d",
                                                5,
                                                30,
                                                input.expiresAt()))
                                .getError()
                                .getCode())
                .isEqualTo(OrderProblem.Code.RESERVATION_REJECTED);
    }

    @Test
    void readAcceptProgressAndReplay() {
        Errand errand = listing();
        when(errands.findById("e")).thenReturn(Optional.of(errand));
        when(errands.lockById("e")).thenReturn(Optional.of(errand));
        when(errands.findByStatusAndExpiresAtAfter(eq(Errand.Status.OPEN), eq(now), any()))
                .thenReturn(new PageImpl<>(List.of(errand)));
        assertThat(workflow.available(1, 20).getData().getTotalElements()).isEqualTo(1);
        assertThat(workflow.getErrand("e").getData().pickup().id()).isEqualTo("p");
        when(suppliers.list())
                .thenReturn(List.of(new SupplierGateway.Supplier("p", "name", "location")));
        assertThat(workflow.suppliers().getData()).hasSize(1);
        ActionInput accept = new ActionInput("accept", "c", 0);
        Order order = workflow.accept("e", accept).getData().order();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(orders.lockById(order.getId())).thenReturn(Optional.of(order));
        assertThat(workflow.getOrder(order.getId()).getData().order()).isSameAs(order);
        assertThat(
                        workflow.progress(
                                        order.getId(),
                                        new ActionInput("start", "c", 0),
                                        Order.Status.IN_PROGRESS)
                                .isError())
                .isFalse();
        workflow.progress(order.getId(), new ActionInput("pickup", "c", 0), Order.Status.PICKED_UP);
        workflow.progress(
                order.getId(), new ActionInput("deliver", "c", 0), Order.Status.DELIVERED);
        CommandReceipt receipt = mock(CommandReceipt.class);
        when(receipt.verify(anyString())).thenReturn(Optional.empty());
        when(commands.findById(anyString())).thenReturn(Optional.of(receipt));
        assertThat(workflow.accept("e", accept).getData().order()).isSameAs(order);
        assertThat(
                        workflow.progress(
                                        order.getId(),
                                        new ActionInput("deliver", "c", 0),
                                        Order.Status.DELIVERED)
                                .getData()
                                .order())
                .isSameAs(order);
        when(receipt.verify(anyString()))
                .thenReturn(Optional.of(OrderProblem.conflict("Different command")));
        assertThat(workflow.accept("e", accept).isError()).isTrue();
        assertThat(workflow.progress(order.getId(), accept, Order.Status.DELIVERED).isError())
                .isTrue();
        verify(checkpoints, times(4)).save(any());
        assertThat(workflow.getErrand("missing").getError().getCode())
                .isEqualTo(OrderProblem.Code.NOT_FOUND);
        assertThat(workflow.getOrder("missing").isError()).isTrue();
        assertThat(workflow.accept("missing", accept).isError()).isTrue();
        assertThat(workflow.progress("missing", accept, Order.Status.IN_PROGRESS).isError())
                .isTrue();
    }

    @Test
    void rejectedCommandsDoNotMutateOrWrite() {
        Errand errand = listing();
        Order order = new Order("o", errand, "c", now);
        when(errands.lockById("e")).thenReturn(Optional.of(errand));
        when(errands.findById("e")).thenReturn(Optional.of(errand));
        when(orders.lockById("o")).thenReturn(Optional.of(order));
        assertThat(workflow.accept("e", new ActionInput("cmd", "r", 0)).getError().getCode())
                .isEqualTo(OrderProblem.Code.FORBIDDEN);
        assertThat(
                        workflow.progress(
                                        "o", new ActionInput("cmd", "c", 0), Order.Status.DELIVERED)
                                .isError())
                .isTrue();
        assertThat(errand.getStatus()).isEqualTo(Errand.Status.OPEN);
        assertThat(order.getStatus()).isEqualTo(Order.Status.ACCEPTED);
        verify(errands, never()).flush();
        verifyNoInteractions(checkpoints, credits);
        verify(orders, never()).saveAndFlush(any());
    }

    @Test
    void missingSuppliersAndListingReturnBeforeMutations() {
        Errand errand = listing();
        Order order = new Order("o", errand, "c", now);
        when(errands.lockById("e")).thenReturn(Optional.of(errand));
        when(orders.lockById("o")).thenReturn(Optional.of(order));
        assertThat(
                        workflow.progress(
                                        "o",
                                        new ActionInput("cmd", "c", 0),
                                        Order.Status.IN_PROGRESS)
                                .isError())
                .isTrue();
        when(errands.findById("e")).thenReturn(Optional.of(errand));
        when(errands.findByStatusAndExpiresAtAfter(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(errand)));
        when(suppliers.resolve("p")).thenReturn(Optional.empty());
        assertThat(workflow.create(input).isError()).isTrue();
        assertThat(workflow.accept("e", new ActionInput("cmd", "c", 0)).isError()).isTrue();
        assertThat(workflow.available(1, 20).isError()).isTrue();
        when(suppliers.resolve("p"))
                .thenReturn(Optional.of(new SupplierGateway.Supplier("p", "n", "l")));
        when(suppliers.resolve("d")).thenReturn(Optional.empty());
        assertThat(workflow.getErrand("e").isError()).isTrue();
        assertThat(errand.getStatus()).isEqualTo(Errand.Status.OPEN);
        assertThat(order.getStatus()).isEqualTo(Order.Status.ACCEPTED);
        verifyNoInteractions(credits, checkpoints);
        verify(orders, never()).flush();
        verify(errands, never()).saveAndFlush(any());
    }

    private Errand listing() {
        return new Errand(
                "e", "r", "Bring my lunch", "p", "d", 5, 30, now.plusSeconds(1800), now, "hash");
    }
}
