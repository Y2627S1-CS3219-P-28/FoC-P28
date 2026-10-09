/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import sg.edu.nus.foc.credit.credit.CreditOutcomeEvent;
import sg.edu.nus.foc.credit.credit.CreditOutcomeType;
import sg.edu.nus.foc.credit.credit.CreditService;
import sg.edu.nus.foc.credit.support.PostgreSqlTestContainer;

@SpringBootTest
class CreditBalanceNotificationIntegrationTest {

    private static final String CHANNEL = "credit_balance_changed";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainer.register(registry);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CreditService credits;

    @BeforeEach
    void reset() {
        jdbc.execute("truncate table credit_ledger, credit_reservations, "
                + "credit_idempotency_records, credit_accounts cascade");
    }

    @Test
    void publishesCommittedInsertsAndRealUpdatesToEveryListener() throws Exception {
        try (Connection first = listen(); Connection second = listen()) {
            insertAccount("user-a");

            assertThat(awaitUserIds(first, 1)).containsExactly("user-a");
            assertThat(awaitUserIds(second, 1)).containsExactly("user-a");

            jdbc.update("update credit_accounts set total_balance = 51 where user_id = ?", "user-a");

            assertThat(awaitUserIds(first, 1)).containsExactly("user-a");
            assertThat(awaitUserIds(second, 1)).containsExactly("user-a");
        }
    }

    @Test
    void suppressesRolledBackAndNoOpChanges() throws Exception {
        try (Connection listener = listen()) {
            insertAccount("user-a");
            assertThat(awaitUserIds(listener, 1)).containsExactly("user-a");

            jdbc.update("update credit_accounts set total_balance = total_balance, "
                    + "reserved_balance = reserved_balance where user_id = ?", "user-a");
            assertThat(receive(listener, 250)).isEmpty();

            try (Connection writer = dataSource.getConnection()) {
                writer.setAutoCommit(false);
                try (PreparedStatement statement = writer.prepareStatement("""
                        insert into credit_accounts
                            (user_id, total_balance, reserved_balance, created_at, updated_at)
                        values (?, 50, 0, current_timestamp, current_timestamp)
                        """)) {
                    statement.setString(1, "rolled-back-user");
                    statement.executeUpdate();
                }
                writer.rollback();
            }
            assertThat(receive(listener, 250)).isEmpty();
        }
    }

    @Test
    void reservationRefundAndCompletionNotifyOnlyAffectedAccounts() throws Exception {
        try (Connection listener = listen()) {
            credits.initializeAccount(UUID.randomUUID(), "requester", Instant.now());
            assertThat(awaitUserIds(listener, 1)).containsExactly("requester");
            credits.initializeAccount(UUID.randomUUID(), "courier", Instant.now());
            assertThat(awaitUserIds(listener, 1)).containsExactly("courier");

            credits.reserve("refund-order", "requester", 10);
            assertThat(awaitUserIds(listener, 1)).containsExactly("requester");
            CreditOutcomeEvent refund = outcome(
                    "refund-event", CreditOutcomeType.OPEN_ORDER_REFUND,
                    "refund-order", "requester", null, 10, "CANCELLED");
            credits.processOutcome(refund);
            assertThat(awaitUserIds(listener, 1)).containsExactly("requester");

            credits.processOutcome(refund);
            assertThat(receive(listener, 250)).isEmpty();

            credits.reserve("completion-order", "requester", 12);
            assertThat(awaitUserIds(listener, 1)).containsExactly("requester");
            credits.assignCourier("completion-order", "courier");
            assertThat(receive(listener, 250)).isEmpty();

            credits.processOutcome(outcome(
                    "completion-event", CreditOutcomeType.ORDER_COMPLETION,
                    "completion-order", "courier", "courier", 12, "COMPLETED"));
            assertThat(awaitUserIds(listener, 2))
                    .containsExactlyInAnyOrder("requester", "courier");
        }
    }

    @Test
    void managedListenerForwardsValidUsersAndSurvivesADeliveryFailure() throws Exception {
        CreditBalanceEventStream stream = mock(CreditBalanceEventStream.class);
        PostgresCreditBalanceListener listener = new PostgresCreditBalanceListener(dataSource, stream);
        listener.start();
        try {
            awaitConnected(listener);
            assertThat(listener.isConnected()).isTrue();

            jdbc.execute("select pg_notify('credit_balance_changed', '')");
            verify(stream, after(250).never()).notifyBalanceChanged("");

            doThrow(new IllegalStateException("bad subscriber"))
                    .when(stream).notifyBalanceChanged("failing-user");
            jdbc.execute("select pg_notify('credit_balance_changed', 'failing-user')");
            jdbc.execute("select pg_notify('credit_balance_changed', 'healthy-user')");

            verify(stream, timeout(2_000)).notifyBalanceChanged("failing-user");
            verify(stream, timeout(2_000)).notifyBalanceChanged("healthy-user");
        } finally {
            listener.stop();
        }
        assertThat(listener.isRunning()).isFalse();
        assertThat(listener.isConnected()).isFalse();
    }

    private Connection listen() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(true);
        try (var statement = connection.createStatement()) {
            statement.execute("listen " + CHANNEL);
        }
        return connection;
    }

    private void insertAccount(String userId) {
        jdbc.update("""
                insert into credit_accounts
                    (user_id, total_balance, reserved_balance, created_at, updated_at)
                values (?, 50, 0, current_timestamp, current_timestamp)
                """, userId);
    }

    private static CreditOutcomeEvent outcome(String eventId, CreditOutcomeType type,
                                               String orderId, String actorId, String courierId,
                                               long amount, String status) {
        return new CreditOutcomeEvent(
                eventId, type, 1, orderId, 1, Instant.now(), actorId,
                "requester", courierId, amount, status, false, null);
    }

    private static List<String> awaitUserIds(Connection connection, int count) throws SQLException {
        List<String> userIds = new ArrayList<>();
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (userIds.size() < count) {
            long remainingMillis = Duration.ofNanos(deadline - System.nanoTime()).toMillis();
            if (remainingMillis <= 0) {
                break;
            }
            userIds.addAll(receive(connection, (int) Math.min(remainingMillis, 500)));
        }
        return userIds;
    }

    private static List<String> receive(Connection connection, int timeoutMillis) throws SQLException {
        PGNotification[] notifications = connection.unwrap(PGConnection.class)
                .getNotifications(timeoutMillis);
        if (notifications == null) {
            return List.of();
        }
        return Arrays.stream(notifications)
                .filter(notification -> CHANNEL.equals(notification.getName()))
                .map(PGNotification::getParameter)
                .toList();
    }

    private static void awaitConnected(PostgresCreditBalanceListener listener) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!listener.isConnected() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
