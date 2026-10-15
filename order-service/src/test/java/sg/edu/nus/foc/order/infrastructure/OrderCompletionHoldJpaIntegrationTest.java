package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import sg.edu.nus.foc.order.application.LifecycleProcessingService;
import sg.edu.nus.foc.order.application.OrderCompletionHoldService;
import sg.edu.nus.foc.order.application.OrderTransitionService;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderCompletionHold;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IAcceptedOrderCancellationTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOpenOrderRefundTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOrderCompletionTaskPublisher;

@SpringBootTest(properties = {
        "spring.profiles.active=local", "order.lifecycle.cron=-", "order.messaging.outbox.recovery-cron=-"
})
@Testcontainers
class OrderCompletionHoldJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("completion_hold_test").withUsername("order_test").withPassword("order_test_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private JpaOrderRepository orders;
    @Autowired private JpaOrderCompletionHoldRepository holds;
    @Autowired private JpaOrderCheckpointRepository checkpoints;
    @Autowired private JpaOrderEventOutboxRepository outbox;
    @Autowired private JpaCommandReceiptRepository receipts;
    @Autowired private OrderCompletionHoldService holdService;
    @Autowired private OrderTransitionService transitions;
    @Autowired private LifecycleProcessingService lifecycle;
    @Autowired private PlatformTransactionManager transactions;

    @MockitoSpyBean private OrderCompletionHoldPersistenceAdapter holdAdapter;
    @MockitoSpyBean private OrderEventOutboxPersistenceAdapter penaltyOutboxAdapter;
    @MockitoBean private sg.edu.nus.foc.order.messagingpublisher.interfaces.IPenaltyScoreAdditionPublisher additionPublisher;
    @MockitoBean private sg.edu.nus.foc.order.messagingpublisher.interfaces.IPenaltyScoreDeductionPublisher deductionPublisher;
    @MockitoBean private IOrderCompletionTaskPublisher completionPublisher;
    @MockitoBean private IOpenOrderRefundTaskPublisher refundPublisher;
    @MockitoBean private IAcceptedOrderCancellationTaskPublisher cancellationPublisher;

    @BeforeEach
    void clearIsolatedDatabase() {
        outbox.deleteAll();
        receipts.deleteAll();
        holds.deleteAll();
        checkpoints.deleteAll();
        orders.deleteAll();
    }

    @Test
    void sqlSelectionSkipsHeldOrdersAndTheUnheldOrderCompletes() {
        Order held = delivered();
        Order successful = delivered();
        long displayedVersion = held.getVersion();
        holdService.place(held.getId(), "held-report", "owner", null);

        OrderProblem rejected = assertThrows(OrderProblem.class, () -> transitions.complete(
                "stale-held-completion", held.getId(), "owner", displayedVersion, null));
        assertEquals("COMPLETION_ON_HOLD", rejected.getCode());

        assertEquals(List.of(successful.getId()), orders.findDueForAutoCompletionIds(
                OrderStatus.DELIVERED, NOW.minus(Order.AUTOMATIC_COMPLETION_DELAY)));
        assertEquals(1, lifecycle.autoCompleteDue(NOW));
        assertEquals(OrderStatus.DELIVERED, orders.findById(held.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.COMPLETED, orders.findById(successful.getId()).orElseThrow().getStatus());
        assertEquals(2, outbox.count());
    }

    @Test
    void placementIsIdempotentAndReportIdsCannotBeMovedToAnotherOrder() {
        Order order = delivered();
        Order first = holdService.place(order.getId(), "report-a", "owner", null);
        long version = first.getVersion();
        holdService.place(order.getId(), "report-a", "owner", null);
        holdService.place(order.getId(), "report-b", "owner", null);

        assertEquals(2, holds.count());
        assertTrue(orders.findById(order.getId()).orElseThrow().isCompletionHold());
        assertEquals(version, orders.findById(order.getId()).orElseThrow().getVersion());
        Order another = delivered();
        assertThrows(OrderProblem.class, () -> holdService.place(another.getId(), "report-a", "owner", null));
        assertFalse(orders.findById(another.getId()).orElseThrow().isCompletionHold());
        assertEquals(0, outbox.count());
    }

    @Test
    void aFailedPlacementRollsBackBothRecordAndFlag() {
        Order order = delivered();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            holds.flush();
            throw new IllegalStateException("failure after database write");
        }).when(holdAdapter).save(any(OrderCompletionHold.class));

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> holdService.place(order.getId(), "failed-report", "owner", null));
        assertEquals("failure after database write", failure.getMessage());
        assertFalse(orders.findById(order.getId()).orElseThrow().isCompletionHold());
        assertFalse(holds.existsById("failed-report"));
    }

    @Test
    void heldOpenOrderIsNotExpiredAndManualFundingOutcomeIsRejected() {
        Order order = orders.saveAndFlush(open());
        holdService.place(order.getId(), "open-report", "owner", null);
        long version = orders.findById(order.getId()).orElseThrow().getVersion();

        assertEquals(0, lifecycle.expireDue(NOW));
        assertThrows(OrderProblem.class, () -> transitions.cancel("cancel-held", order.getId(), "owner", version, null));
        assertEquals(OrderStatus.OPEN, orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
    }

    @Test
    void holdAndCompletionSerializeOnTheSameOrderRow() throws Exception {
        Order order = delivered();
        CountDownLatch placed = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> placement = worker.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                holdService.place(order.getId(), "race-report", "owner", null);
                holds.flush();
                placed.countDown();
                try {
                    if (!commit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting to commit hold");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }));
            assertTrue(placed.await(10, TimeUnit.SECONDS));
            assertThrows(RuntimeException.class, () -> transitions.autoComplete(order.getId(), NOW));
            commit.countDown();
            placement.get(10, TimeUnit.SECONDS);
            assertFalse(transitions.autoComplete(order.getId(), NOW));
            assertEquals(OrderStatus.DELIVERED, orders.findById(order.getId()).orElseThrow().getStatus());
            assertEquals(0, outbox.count());
        } finally {
            commit.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void concurrentSameReportCannotMoveItsOrderBinding() throws Exception {
        Order firstOrder = delivered();
        Order secondOrder = delivered();
        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            bothRead.countDown();
            if (!bothRead.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Both placements did not read");
            return result;
        }).when(holdAdapter).findByReportId("shared-report");
        doAnswer(invocation -> {
            OrderCompletionHold hold = invocation.getArgument(0);
            if (hold.getOrderId().equals(secondOrder.getId()) && !firstCommitted.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("First placement did not commit");
            }
            return invocation.callRealMethod();
        }).when(holdAdapter).save(any(OrderCompletionHold.class));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Order> first = workers.submit(() -> {
                try {
                    return holdService.place(firstOrder.getId(), "shared-report", "owner", null);
                } finally {
                    firstCommitted.countDown();
                }
            });
            Future<Order> second = workers.submit(() -> holdService.place(secondOrder.getId(), "shared-report", "owner", null));
            assertTrue(first.get(20, TimeUnit.SECONDS).isCompletionHold());
            assertThrows(ExecutionException.class, () -> second.get(20, TimeUnit.SECONDS));
            assertEquals(firstOrder.getId(), holds.findById("shared-report").orElseThrow().getOrderId());
            assertFalse(orders.findById(secondOrder.getId()).orElseThrow().isCompletionHold());
            assertEquals(1, holds.count());
        } finally {
            firstCommitted.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void penaltyPersistenceFailureRollsBackCompletionCreditEventAndReceiptTogether() {
        Order order = delivered();
        long version = orders.findById(order.getId()).orElseThrow().getVersion();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("injected penalty persistence failure");
        }).when(penaltyOutboxAdapter).enqueue(org.mockito.ArgumentMatchers.isA(
                sg.edu.nus.foc.order.messagingpublisher.dto.PenaltyScoreAdditionEvent.class));

        assertThrows(RuntimeException.class,
                () -> transitions.complete("penalty-rollback", order.getId(), "owner", version, null));
        assertEquals(OrderStatus.DELIVERED, orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(0, outbox.count());
        assertEquals(0, receipts.count());
        assertFalse(checkpoints.findByOrderIdOrderByOccurredAtAsc(order.getId()).stream()
                .anyMatch(checkpoint -> checkpoint.getStatus() == OrderStatus.COMPLETED));
    }

    private Order delivered() {
        Order order = open();
        Instant start = NOW.minusSeconds(3 * 86400L);
        order.accept("courier", 0, start.plusSeconds(60));
        order.start("courier", 0);
        order.markPickedUp("courier", 0);
        order.markDelivered("courier", 0);
        Order saved = orders.saveAndFlush(order);
        checkpoints.saveAndFlush(new OrderCheckpoint(saved.getId(), OrderStatus.ACCEPTED,
                start.plusSeconds(60), "courier", null));
        checkpoints.saveAndFlush(new OrderCheckpoint(saved.getId(), OrderStatus.DELIVERED,
                NOW.minusSeconds(49 * 3600L), "courier", null));
        return saved;
    }

    private static Order open() {
        Instant start = NOW.minusSeconds(3 * 86400L);
        return Order.open("owner", "books", "pickup", "delivery", 5, 30, start, start.plusSeconds(3600));
    }
}
