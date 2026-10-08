package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;

@SpringBootTest(properties = {
    "spring.profiles.active=local",
    "order.messaging.outbox.recovery-cron=-",
    "order.lifecycle.expiry-cron=-",
    "order.lifecycle.auto-completion-cron=-"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class OrderRequesterVisibilityJpaIntegrationTest {
    private static final Instant START = Instant.parse("2026-10-08T00:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_requester_visibility_test")
            .withUsername("order_test")
            .withPassword("order_test_password");

    @Autowired
    private JpaOrderRepository orders;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void hidesOnlySuccessfullyRepostedExpiredRowsAndCountsBeforePagination() {
        Order superseded = expired("requester", START);
        Order repost = superseded.createRepost("new item", 4, 30,
                START.plusSeconds(3601), START.plusSeconds(7201));
        superseded.linkRepost(repost.getId());
        Order unreposted = expired("requester", START.plusSeconds(1));
        Order otherRequester = expired("someone-else", START.plusSeconds(2));
        orders.saveAllAndFlush(List.of(superseded, repost, unreposted, otherRequester));

        Page<Order> first = orders.findByRequesterIdOrderByCreatedAtDesc("requester", PageRequest.of(0, 1));
        Page<Order> second = orders.findByRequesterIdOrderByCreatedAtDesc("requester", PageRequest.of(1, 1));

        assertEquals(2, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(repost.getId(), first.getContent().get(0).getId());
        assertEquals(unreposted.getId(), second.getContent().get(0).getId());
        assertEquals(OrderStatus.EXPIRED, second.getContent().get(0).getStatus());
        assertTrue(orders.findById(superseded.getId()).isPresent(), "History/refund references must remain");
    }

    @Test
    void emptyRequesterPageHasNoLeakedOrdersOrTotals() {
        orders.saveAndFlush(expired("another-requester", START));

        Page<Order> result = orders.findByRequesterIdOrderByCreatedAtDesc("requester", PageRequest.of(0, 20));

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    private Order expired(String requester, Instant createdAt) {
        Order order = Order.open(requester, "item", "pickup", "delivery", 3, 30,
                createdAt, createdAt.plusSeconds(3600));
        order.expire(0, createdAt.plusSeconds(3600));
        return order;
    }
}
