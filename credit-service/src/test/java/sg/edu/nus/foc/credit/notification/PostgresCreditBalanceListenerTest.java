/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;

class PostgresCreditBalanceListenerTest {

    @Test
    void reconnectsAfterADatabaseFailureAndStopsCleanly() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PGConnection postgres = mock(PGConnection.class);
        CreditBalanceEventStream stream = mock(CreditBalanceEventStream.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("database unavailable"))
                .thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.unwrap(PGConnection.class)).thenReturn(postgres);
        when(connection.isClosed()).thenReturn(false);
        when(postgres.getNotifications(anyInt())).thenReturn(null);
        PostgresCreditBalanceListener listener = new PostgresCreditBalanceListener(dataSource, stream);

        listener.start();
        listener.start();
        try {
            verify(dataSource, timeout(2_500).times(2)).getConnection();
            awaitConnected(listener);
            assertThat(listener.isRunning()).isTrue();
            assertThat(listener.isConnected()).isTrue();
        } finally {
            listener.stop();
            listener.stop();
        }

        assertThat(listener.isRunning()).isFalse();
        assertThat(listener.isConnected()).isFalse();
        verify(connection).close();
    }

    private static void awaitConnected(PostgresCreditBalanceListener listener) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!listener.isConnected() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
