/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Code generation.
 * Scope: Implemented postgres credit balance listener based on provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.notification;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class PostgresCreditBalanceListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PostgresCreditBalanceListener.class);
    private static final String CHANNEL = "credit_balance_changed";
    private static final int NOTIFICATION_WAIT_MILLIS = 10_000;
    private static final long MAX_RECONNECT_DELAY_MILLIS = 30_000;

    private final DataSource dataSource;
    private final CreditBalanceEventStream eventStream;

    private volatile boolean running;
    private volatile boolean connected;
    private volatile Connection connection;
    private Thread listenerThread;

    public PostgresCreditBalanceListener(DataSource dataSource, CreditBalanceEventStream eventStream) {
        this.dataSource = dataSource;
        this.eventStream = eventStream;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        listenerThread = Thread.ofVirtual().name("credit-balance-listener").start(this::listen);
    }

    @Override
    public synchronized void stop() {
        running = false;
        closeConnection();
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    boolean isConnected() {
        return connected;
    }

    private void listen() {
        long reconnectDelay = 1_000;
        while (running) {
            try {
                listenOnConnection();
                reconnectDelay = 1_000;
            } catch (SQLException | RuntimeException exception) {
                if (!running) {
                    return;
                }
                if (connected) {
                    reconnectDelay = 1_000;
                }
                log.atWarn()
                        .addKeyValue("service", "credit-service")
                        .addKeyValue("operation", "listen_credit_balance_notifications")
                        .addKeyValue("reconnectDelayMillis", reconnectDelay)
                        .addKeyValue("errorType", exception.getClass().getSimpleName())
                        .addKeyValue("errorMessage", exception.getMessage())
                        .log("credit_balance_listener_disconnected");
                sleep(reconnectDelay);
                reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY_MILLIS);
            } finally {
                closeConnection();
            }
        }
    }

    private void listenOnConnection() throws SQLException {
        Connection acquired = dataSource.getConnection();
        connection = acquired;
        acquired.setAutoCommit(true);
        try (Statement statement = acquired.createStatement()) {
            statement.execute("listen " + CHANNEL);
        }
        PGConnection postgres = acquired.unwrap(PGConnection.class);
        connected = true;
        log.info("credit_balance_listener_connected channel={}", CHANNEL);

        while (running && !acquired.isClosed()) {
            PGNotification[] notifications = postgres.getNotifications(NOTIFICATION_WAIT_MILLIS);
            if (notifications == null) {
                continue;
            }
            for (PGNotification notification : notifications) {
                String userId = notification.getParameter();
                if (CHANNEL.equals(notification.getName()) && userId != null && !userId.isBlank()) {
                    try {
                        eventStream.notifyBalanceChanged(userId);
                    } catch (RuntimeException exception) {
                        log.atWarn()
                                .addKeyValue("service", "credit-service")
                                .addKeyValue("operation", "deliver_credit_balance_notification")
                                .addKeyValue("userId", userId)
                                .addKeyValue("errorType", exception.getClass().getSimpleName())
                                .log("credit_balance_notification_delivery_failed");
                    }
                }
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void closeConnection() {
        connected = false;
        Connection current = connection;
        connection = null;
        if (current == null) {
            return;
        }
        try {
            current.close();
        } catch (SQLException exception) {
            log.debug("Could not close the credit balance listener connection", exception);
        }
    }
}
