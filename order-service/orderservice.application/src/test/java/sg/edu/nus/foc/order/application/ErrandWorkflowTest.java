package sg.edu.nus.foc.order.application;
import sg.edu.nus.foc.order.domain.*;
import sg.edu.nus.foc.order.application.ErrandWorkflow.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.data.domain.*;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ErrandWorkflowTest {
    ErrandRepository errands=mock(ErrandRepository.class);
    OrderRepository orders=mock(OrderRepository.class);
    CheckpointRepository checkpoints=mock(CheckpointRepository.class);
    CommandReceiptRepository commands=mock(CommandReceiptRepository.class);
    SupplierGateway suppliers=mock(SupplierGateway.class);
    CreditGateway credits=mock(CreditGateway.class);
    Instant now=Instant.parse("2026-09-29T12:00:00Z");
    ErrandWorkflow workflow=new ErrandWorkflow(errands,orders,checkpoints,commands,suppliers,credits,Clock.fixed(now,ZoneOffset.UTC));
    CreateInput input=new CreateInput("cmd","r","Bring my lunch","p","d",5,30,now.plusSeconds(1800));
    @BeforeEach void setup() {
        TransactionSynchronizationManager.initSynchronization();
        when(credits.reserve(anyString(),anyString(),anyLong())).thenReturn(true);
        when(suppliers.resolve(anyString())).thenAnswer(i->new SupplierGateway.Supplier(i.getArgument(0),"name","location"));
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clearSynchronization(); }
    @Test void creationRetryValidationAndCompensation() {
        var created=workflow.create(input).errand();
        assertThat(created.getStatus()).isEqualTo("OPEN");
        var synchronization=TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(credits,never()).release(anyString(),anyString());
        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(credits).release(created.getId(),"r");
        when(errands.findById(created.getId())).thenReturn(Optional.of(created));
        assertThat(workflow.create(input).errand()).isSameAs(created);
        assertThatThrownBy(()->workflow.create(new CreateInput("cmd","r","Changed description","p","d",5,30,input.expiresAt()))).isInstanceOf(OrderProblem.class);
        assertThatThrownBy(()->workflow.create(new CreateInput("cmd2","r","Bad\ntext","p","p",5,30,now))).isInstanceOf(OrderProblem.class)
            .satisfies(e->assertThat(((OrderProblem)e).getDetails()).hasSize(3));
        when(credits.reserve(anyString(),anyString(),anyLong())).thenReturn(false);
        assertThatThrownBy(()->workflow.create(new CreateInput("cmd3","r","Bring my lunch","p","d",5,30,input.expiresAt()))).isInstanceOf(OrderProblem.class);
    }
    @Test void readAcceptProgressAndReplay() {
        var e=new Errand("e","r","Bring my lunch","p","d",5,30,now.plusSeconds(1800),now,"hash");
        when(errands.findById("e")).thenReturn(Optional.of(e));
        when(errands.lockById("e")).thenReturn(Optional.of(e));
        when(errands.findByStatusAndExpiresAtAfter(eq("OPEN"),eq(now),any())).thenReturn(new PageImpl<>(List.of(e)));
        assertThat(workflow.available(1,20).getTotalElements()).isEqualTo(1);
        assertThat(workflow.getErrand("e").pickup().id()).isEqualTo("p");
        when(suppliers.list()).thenReturn(List.of(new SupplierGateway.Supplier("p","name","location")));
        assertThat(workflow.suppliers()).hasSize(1);
        var accept=new ActionInput("accept","c",0);
        var order=workflow.accept("e",accept).order();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(orders.lockById(order.getId())).thenReturn(Optional.of(order));
        when(checkpoints.findByOrderIdOrderByOccurredAtAscIdAsc(order.getId())).thenReturn(List.of());
        assertThat(workflow.getOrder(order.getId()).order()).isSameAs(order);
        workflow.progress(order.getId(),new ActionInput("start","c",0),Order.Status.IN_PROGRESS);
        workflow.progress(order.getId(),new ActionInput("pickup","c",0),Order.Status.PICKED_UP);
        workflow.progress(order.getId(),new ActionInput("deliver","c",0),Order.Status.DELIVERED);
        when(commands.findById(anyString())).thenReturn(Optional.of(mock(CommandReceipt.class)));
        assertThat(workflow.accept("e",accept).order()).isSameAs(order);
        assertThat(workflow.progress(order.getId(),new ActionInput("deliver","c",0),Order.Status.DELIVERED).order()).isSameAs(order);
        verify(checkpoints,times(4)).save(any());
        assertThatThrownBy(()->workflow.getErrand("missing")).isInstanceOf(OrderProblem.class);
        assertThatThrownBy(()->workflow.getOrder("missing")).isInstanceOf(OrderProblem.class);
        assertThatThrownBy(()->workflow.accept("missing",accept)).isInstanceOf(OrderProblem.class);
        assertThatThrownBy(()->workflow.progress("missing",accept,Order.Status.IN_PROGRESS)).isInstanceOf(OrderProblem.class);
    }
}
