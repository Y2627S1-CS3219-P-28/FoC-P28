package sg.edu.nus.foc.order.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sg.edu.nus.foc.order.application.recovery.*;
import sg.edu.nus.foc.order.adapter.MockPeerAdapters;
import sg.edu.nus.foc.order.api.dto.response.OrderResponse;
import sg.edu.nus.foc.order.application.OrderLifecycleScheduler;
import sg.edu.nus.foc.order.domain.OrderProblem;

/** F1/F3/F11/F13, ADR-033: real PostgreSQL commands; Credit is a contract stub. */
@SpringBootTest(properties = {"spring.profiles.active=local", "order.peers.mode=mock",
    "order.commands.enabled=true", "order.lifecycle.cron=-",
    "order.messaging.outbox.recovery-cron=-", "debug=false", "logging.level.root=WARN",
    "logging.level.org.hibernate.SQL=OFF"})
@Testcontainers
class OrderCommandRecoveryIntegrationTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", DB::getJdbcUrl);
        p.add("spring.datasource.username", DB::getUsername);
        p.add("spring.datasource.password", DB::getPassword);
    }
    @Autowired OrderCommandService service;
    @Autowired OrderCommandStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockPeerAdapters balances;
    @MockitoSpyBean LocalCreditCommandStub credit;
    @MockitoSpyBean sg.edu.nus.foc.order.application.OrderCreationService creation;
    @Autowired sg.edu.nus.foc.order.application.OrderTransitionService transitions;
    @Autowired sg.edu.nus.foc.order.application.LifecycleProcessingService lifecycle;
    @Autowired OrderLifecycleScheduler scheduler;

    CommandRequest create(String actor) {
        return new CommandRequest("CREATE", actor, null, 0,
            new CommandRequest.Creation("test", "pickup", "delivery", 5, 15,
                Instant.now().plusSeconds(7200), false, null, 0, 0, null));
    }
    String key() { return UUID.randomUUID().toString(); }
    void due(String k) { jdbc.update("update order_commands set next_retry_at=clock_timestamp()-interval '1 second', owner=null, lease_expires_at=null where command_id=?", k); }

    @Test void creationIsDurableBeforeCreditAndTerminalReplayDoesNotReserveTwice() {
        String k = key(), actor = key();
        doAnswer(call -> {
            assertEquals("PENDING", store.get(k).status());
            assertNotNull(store.get(k).owner());
            assertEquals(0, jdbc.queryForObject("select count(*) from orders where id=?", Integer.class, store.get(k).orderId()));
            return call.callRealMethod();
        }).when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        CommandRequest input = create(actor);
        var result = service.submit(k, input, null);
        assertEquals("SUCCESS", result.outcome());
        assertEquals("OPEN", result.result().getStatus());
        assertEquals(result.orderId(), service.submit(k, input, null).orderId());
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
        assertEquals(1, result.attemptCount());
    }

    @Test void immutableInputAndAccountBindingRejectKeyReuse() {
        String k = key(); var input = create(key());
        service.submit(k, input, null);
        assertThrows(OrderProblem.class, () -> service.submit(k, create(key()), null));
        assertThrows(OrderProblem.class, () -> service.status(k, "other", null));
        var changed = new CommandRequest("ACCEPT", input.actorId(), key(), 0, null);
        assertThrows(OrderProblem.class, () -> service.submit(k, changed, null));
    }

    @Test void lostCreditResponseReplaysSameCandidateAndFinishesOnce() {
        String k = key(), actor = key(); var input = create(actor);
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost response"); })
            .doCallRealMethod().when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        var pending = service.submit(k, input, null);
        assertEquals("PENDING", pending.status());
        assertNull(pending.result());
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
        due(k);
        var success = service.resume(k, actor, null);
        assertEquals("SUCCESS", success.outcome());
        assertEquals(pending.orderId(), success.orderId());
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
    }

    @Test void laterAuthorizationFailureDoesNotResolveEarlierTimeout() {
        String k = key(), actor = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost response"); })
            .doThrow(new OrderProblem("UNAUTHENTICATED", "expired"))
            .doCallRealMethod().when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        service.submit(k, create(actor), null); due(k);
        assertEquals("AUTHORIZATION_REQUIRED", service.resume(k, actor, null).reason());
        assertEquals("PENDING", store.get(k).status());
        due(k);
        assertEquals("SUCCESS", service.resume(k, actor, "fresh token").outcome());
    }

    @Test void concurrentSameKeyHasOneClaimAndOneRemoteEffect() throws Exception {
        String k = key(), actor = key(); var input = create(actor);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertTrue(release.await(10, TimeUnit.SECONDS)); return call.callRealMethod(); })
            .when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<CommandView> first = pool.submit(() -> service.submit(k, input, null));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            try { assertEquals("PENDING", service.submit(k, input, null).status()); }
            finally { release.countDown(); }
            assertEquals("SUCCESS", first.get(10, TimeUnit.SECONDS).outcome());
        }
        verify(credit, times(1)).execute(eq(k), any(), anyLong(), nullable(String.class));
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
    }

    @Test void acceptanceAndAbortUseCurrentOrderAndKeepOneHistoryAttempt() {
        var opened = service.submit(key(), create(key()), null).result();
        String courier = key();
        var accepted = service.submit(key(), new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null).result();
        assertEquals("ACCEPTED", accepted.getStatus());
        String abortKey = key();
        var aborted = service.submit(abortKey, new CommandRequest("ABORT", courier, accepted.getId(), accepted.getVersion(), null), null);
        assertEquals("OPEN", aborted.result().getStatus());
        assertEquals(opened.getId(), aborted.orderId());
        assertNull(balances.reservationCourier(opened.getId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from order_courier_attempts where order_id=?", Integer.class, opened.getId()));
    }

    @Test void oldWorkerCannotCompleteAfterLeaseTakeover() {
        String k = key(); store.register(k, create(key()));
        var first = store.claim(k, "first", 120, false).orElseThrow();
        assertTrue(store.claim(k, "second", 120, false).isEmpty());
        jdbc.update("update order_commands set lease_expires_at=clock_timestamp()-interval '1 second' where command_id=?", k);
        var second = store.claim(k, "second", 120, false).orElseThrow();
        assertTrue(second.generation() > first.generation());
        assertThrows(OrderProblem.class, () -> store.assertOwned(first));
        store.assertOwned(second);
    }

    @Test void localBusinessRollbackLeavesDurableCommandForForwardRecovery() {
        String k = key(), actor = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("crash before local commit"); })
            .doCallRealMethod().when(creation).createConfirmed(eq(k), anyString(), eq(actor), anyString(),
                anyString(), anyString(), anyLong(), anyInt(), any(), nullable(sg.edu.nus.foc.order.domain.RepostPlan.class), nullable(String.class));
        var pending = service.submit(k, create(actor), null);
        assertEquals("PENDING", pending.status());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders where id=?", Integer.class, pending.orderId()));
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
        due(k); service.recoverDue();
        assertEquals("SUCCESS", store.get(k).outcome());
        assertEquals(5, balances.creditSnapshot(actor).getReserved());
    }

    @Test void insufficientFundsIsAnAuthoritativeTerminalRejection() {
        String actor = key();
        balances.reserve(key(), actor, 49, null);
        var rejected = service.submit(key(), create(actor), null);
        assertEquals("REJECTED", rejected.outcome());
        assertEquals("INSUFFICIENT_CREDITS", rejected.reason());
        assertNull(rejected.result());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders where id=?", Integer.class, rejected.orderId()));
    }

    @Test void invalidDetailsStopBeforeAnyCreditWrite() {
        String k = key(), actor = key();
        var invalid = new CommandRequest("CREATE", actor, null, 0,
            new CommandRequest.Creation("bad", "same", "same", 5, 15, Instant.now().plusSeconds(7200), false, null, 0, 0, null));
        assertEquals("REJECTED", service.submit(k, invalid, null).outcome());
        verify(credit, never()).execute(eq(k), any(), anyLong(), nullable(String.class));
    }

    @Test void expiredRemoteAcceptanceIsCompensatedNotSilentlyAccepted() {
        var opened = service.submit(key(), create(key()), null).result();
        String k = key(), courier = key();
        doAnswer(call -> {
            Object result = call.callRealMethod();
            jdbc.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
            return result;
        }).when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        var result = service.submit(k, new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null);
        assertEquals("REJECTED", result.outcome());
        assertNull(balances.reservationCourier(opened.getId()));
        assertEquals("OPEN", jdbc.queryForObject("select status from orders where id=?", String.class, opened.getId()));
    }

    @Test void unresolvedAcceptanceGuardsOtherKeysCancellationAndExpiry() {
        var opened = service.submit(key(), create(key()), null).result();
        String k = key(), courier = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("response lost"); })
            .when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        service.submit(k, new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null);
        assertThrows(OrderProblem.class, () -> service.submit(key(), new CommandRequest("ACCEPT", key(), opened.getId(), opened.getVersion(), null), null));
        assertThrows(OrderProblem.class, () -> transitions.cancel(key(), opened.getId(), opened.getRequesterId(), opened.getVersion(), null));
        jdbc.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
        lifecycle.expireDue(Instant.now());
        assertEquals("OPEN", jdbc.queryForObject("select status from orders where id=?", String.class, opened.getId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from order_event_outbox where order_id=?", Integer.class, opened.getId()));
    }

    @Test
    void mergedTickResolvesValidAcceptanceWithoutExpiringTheAcceptedOrder() {
        OrderResponse opened = service.submit(key(), create(key()), null).result();
        String acceptanceKey = key();
        String courier = key();
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("response lost");
        }).doCallRealMethod().when(credit).execute(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        CommandView pending = service.submit(acceptanceKey,
                new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null);
        assertEquals("PENDING", pending.status());
        due(acceptanceKey);

        scheduler.processDueOrders();

        assertEquals("SUCCESS", store.get(acceptanceKey).outcome());
        assertEquals("ACCEPTED", jdbc.queryForObject(
                "select status from orders where id=?", String.class, opened.getId()));
        assertEquals(courier, balances.reservationCourier(opened.getId()));
        jdbc.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
        scheduler.processDueOrders();
        assertEquals("ACCEPTED", jdbc.queryForObject(
                "select status from orders where id=?", String.class, opened.getId()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from order_event_outbox where order_id=?", Integer.class, opened.getId()));
    }

    @Test
    void mergedTickCompensatesExpiredAcceptanceBeforeQueuingOneRefund() {
        OrderResponse opened = service.submit(key(), create(key()), null).result();
        String acceptanceKey = key();
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("response lost");
        }).when(credit).execute(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        service.submit(acceptanceKey,
                new CommandRequest("ACCEPT", key(), opened.getId(), opened.getVersion(), null), null);
        jdbc.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
        due(acceptanceKey);

        scheduler.processDueOrders();

        assertEquals("COMPLETED", store.get(acceptanceKey).status());
        assertEquals("REJECTED", store.get(acceptanceKey).outcome());
        assertNull(balances.reservationCourier(opened.getId()));
        assertEquals("EXPIRED", jdbc.queryForObject(
                "select status from orders where id=?", String.class, opened.getId()));
        verify(credit, times(1)).compensate(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from order_event_outbox where order_id=? and event_type='OpenOrderRefundTaskEvent'",
                Integer.class, opened.getId()));

        scheduler.processDueOrders();
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from order_event_outbox where order_id=?", Integer.class, opened.getId()));
    }

    @Test
    void mergedTickKeepsExpiryBlockedUntilCompensationIsConfirmed() {
        OrderResponse opened = service.submit(key(), create(key()), null).result();
        String acceptanceKey = key();
        String courier = key();
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("response lost");
        }).when(credit).execute(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        doReturn(false).when(credit).compensate(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        service.submit(acceptanceKey,
                new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null);
        jdbc.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
        due(acceptanceKey);

        scheduler.processDueOrders();

        assertEquals("PENDING", store.get(acceptanceKey).status());
        assertEquals("RECONCILIATION_REQUIRED", store.get(acceptanceKey).reason());
        assertEquals("OPEN", jdbc.queryForObject(
                "select status from orders where id=?", String.class, opened.getId()));
        assertEquals(courier, balances.reservationCourier(opened.getId()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from order_event_outbox where order_id=?", Integer.class, opened.getId()));

        doCallRealMethod().when(credit).compensate(eq(acceptanceKey), any(), anyLong(), nullable(String.class));
        due(acceptanceKey);
        scheduler.processDueOrders();
        assertEquals("REJECTED", store.get(acceptanceKey).outcome());
        assertEquals("EXPIRED", jdbc.queryForObject(
                "select status from orders where id=?", String.class, opened.getId()));
    }

    @Test void unresolvedAbortGuardsStartAndDoesNotCreateHistoryPrematurely() {
        var opened = service.submit(key(), create(key()), null).result(); String courier = key();
        var accepted = service.submit(key(), new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), null).result();
        String k = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("response lost"); })
            .doCallRealMethod().when(credit).execute(eq(k), any(), anyLong(), nullable(String.class));
        var pending = service.submit(k, new CommandRequest("ABORT", courier, accepted.getId(), accepted.getVersion(), null), null);
        assertEquals("PENDING", pending.status());
        assertThrows(OrderProblem.class, () -> transitions.start(key(), accepted.getId(), courier, accepted.getVersion(), null));
        assertEquals(0, jdbc.queryForObject("select count(*) from order_courier_attempts where order_id=?", Integer.class, accepted.getId()));
        due(k); service.recoverDue();
        assertEquals("SUCCESS", store.get(k).outcome());
    }
}
