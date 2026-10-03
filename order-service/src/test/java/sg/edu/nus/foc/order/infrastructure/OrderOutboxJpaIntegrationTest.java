package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderEventOutbox;
import sg.edu.nus.foc.order.domain.OutboxState;
import sg.edu.nus.foc.order.domain.repository.OrderEventOutboxRepository;
import sg.edu.nus.foc.order.messagingpublisher.dto.OrderCompletionTaskEvent;

@SpringBootTest(properties = {
    "spring.profiles.active=local",
    "order.messaging.outbox.recovery-cron=-"
})
@Testcontainers(disabledWithoutDocker = true)
class OrderOutboxJpaIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_outbox_jpa_test")
            .withUsername("order_test")
            .withPassword("order_test_password");

    @Autowired
    private JpaOrderRepository orders;

    @Autowired
    private JpaOrderEventOutboxRepository jpaOutbox;

    @Autowired
    private OrderEventOutboxRepository outbox;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywaySchemaValidatesAndJpaOutboxCanClaimRetryAndPublish() {
        Instant now = Instant.now();
        Order order = Order.open(
                "requester-1",
                "item",
                "pickup",
                "delivery",
                5,
                15,
                now,
                now.plusSeconds(3600));
        orders.saveAndFlush(order);

        OrderCompletionTaskEvent event = new OrderCompletionTaskEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("OrderCompletionTaskEvent");
        event.setEventVersion(1);
        event.setOrderId(order.getId());
        event.setOrderVersion(1);
        event.setOccurredAt(now);
        event.setActorId("requester-1");
        outbox.enqueue(event);

        List<OrderEventOutbox> claimed = outbox.claimDue(now.plusSeconds(1), now.plusSeconds(60), 10);

        assertEquals(1, claimed.size());
        assertEquals(OutboxState.IN_PROGRESS, claimed.get(0).getState());
        assertEquals(1, claimed.get(0).getAttemptCount());
        assertNotNull(claimed.get(0).getPayload());

        outbox.scheduleRetry(event.getEventId(), now.plusSeconds(2), "temporary failure");
        OrderEventOutbox retried = outbox.claim(event.getEventId(), now.plusSeconds(3), now.plusSeconds(63))
                .orElseThrow();
        assertEquals(2, retried.getAttemptCount());
        outbox.markPublished(event.getEventId(), now.plusSeconds(4));

        OrderEventOutbox published = jpaOutbox.findById(event.getEventId()).orElseThrow();
        assertEquals(OutboxState.PUBLISHED, published.getState());
        assertEquals(2, published.getAttemptCount());
        assertNull(published.getLastError());
    }

    @Test
    void databaseRollbackRemovesBothOrderAndItsOutboxIntent() {
        Instant now = Instant.now();
        Order order = Order.open(
                "requester-rollback",
                "rollback item",
                "pickup-rollback",
                "delivery-rollback",
                2,
                15,
                now,
                now.plusSeconds(3600));
        OrderCompletionTaskEvent event = new OrderCompletionTaskEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("OrderCompletionTaskEvent");
        event.setEventVersion(1);
        event.setOrderId(order.getId());
        event.setOrderVersion(1);
        event.setOccurredAt(now);
        event.setActorId("requester-rollback");

        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            orders.saveAndFlush(order);
            outbox.enqueue(event);
            throw new IllegalStateException("force rollback");
        }));

        assertFalse(orders.existsById(order.getId()));
        assertFalse(jpaOutbox.existsById(event.getEventId()));
    }
}
