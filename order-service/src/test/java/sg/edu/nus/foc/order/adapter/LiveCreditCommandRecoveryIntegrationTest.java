package sg.edu.nus.foc.order.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import sg.edu.nus.foc.order.api.dto.response.OrderResponse;
import sg.edu.nus.foc.order.application.OrderCreationService;
import sg.edu.nus.foc.order.application.OrderTransitionService;
import sg.edu.nus.foc.order.application.OrderLifecycleScheduler;
import sg.edu.nus.foc.order.application.recovery.CommandRequest;
import sg.edu.nus.foc.order.application.recovery.CommandView;
import sg.edu.nus.foc.order.application.recovery.OrderCommandService;
import sg.edu.nus.foc.order.application.recovery.OrderCommandStore;
import sg.edu.nus.foc.order.domain.OrderProblem;

/** Actual Credit container/HTTP/ledger and two isolated PostgreSQL databases.
 * User/Supplier validation is fixture-backed; Pub/Sub and production IAM are not exercised.
 */
@SpringBootTest(properties = {"spring.profiles.active=local", "order.peers.mode=http",
        "order.commands.enabled=true", "order.lifecycle.cron=-", "order.messaging.outbox.recovery-cron=-",
        "logging.level.root=WARN", "order.peers.connect-timeout=3s", "order.peers.read-timeout=5s"})
@Testcontainers
class LiveCreditCommandRecoveryIntegrationTest {
    private static final Network NETWORK = Network.newNetwork();
    @Container
    static final PostgreSQLContainer<?> CREDIT_DB = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(NETWORK).withNetworkAliases("credit-db");
    @Container
    static final PostgreSQLContainer<?> ORDER_DB = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    static final GenericContainer<?> CREDIT = new GenericContainer<>(creditImage())
            .withNetwork(NETWORK).dependsOn(CREDIT_DB).withExposedPorts(8080)
            .withCreateContainerCmdModifier(command -> command.getHostConfig().withPublishAllPorts(false)
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(freePort()), new ExposedPort(8080))))
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://credit-db:5432/" + CREDIT_DB.getDatabaseName())
            .withEnv("SPRING_DATASOURCE_USERNAME", CREDIT_DB.getUsername())
            .withEnv("SPRING_DATASOURCE_PASSWORD", CREDIT_DB.getPassword())
            .withEnv("FIREBASE_AUTH_PROJECT_ID", "demo-foc")
            .withEnv("FIREBASE_AUTH_EMULATOR_HOST", "local-fixture:9099")
            .withEnv("USER_SERVICE_MODE", "mock").withEnv("LOG_LEVEL", "WARN")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(2));

    private static DockerImageName creditImage() {
        Path source = Path.of(System.getProperty("credit.contract.source", "../credit-service")).toAbsolutePath().normalize();
        String image = "foc-credit-command-contract:" + UUID.randomUUID();
        try {
            // Use the standard BuildKit CLI and actual Dockerfile, which contains
            // cache mounts not supported by Testcontainers' legacy image builder.
            Process build = new ProcessBuilder("docker", "build", "-t", image, source.toString())
                    .redirectErrorStream(true).redirectOutput(Path.of("target", "credit-provider-build.log").toFile()).start();
            if (!build.waitFor(5, TimeUnit.MINUTES)) {
                build.destroyForcibly();
                throw new IllegalStateException("Actual Credit test image build timed out.");
            }
            if (build.exitValue() != 0) {
                throw new IllegalStateException("Actual Credit test image build failed.");
            }
            return DockerImageName.parse(image);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Docker CLI and actual Credit source are required for this contract test.", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Credit test image build interrupted.", interrupted);
        }
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot allocate the isolated Credit test port.", failure);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", ORDER_DB::getJdbcUrl);
        properties.add("spring.datasource.username", ORDER_DB::getUsername);
        properties.add("spring.datasource.password", ORDER_DB::getPassword);
        properties.add("order.peers.credit-url", () -> "http://" + CREDIT.getHost() + ":" + CREDIT.getMappedPort(8080));
    }

    @Autowired OrderCommandService commands;
    @Autowired OrderCommandStore store;
    @Autowired JdbcTemplate orderDb;
    @Autowired OrderTransitionService transitions;
    @Autowired OrderLifecycleScheduler scheduler;
    @MockitoSpyBean HttpCreditCommandGateway gateway;
    @MockitoSpyBean HttpPeerAdapters peers;
    @MockitoSpyBean OrderCreationService creation;
    @MockitoBean sg.edu.nus.foc.order.application.OrderOutboxDispatcher dispatcher;

    @BeforeEach
    void validationFixtures() {
        doAnswer(call -> call.getArgument(0)).when(peers).verifyRequester(anyString(), nullable(String.class));
        doAnswer(call -> call.getArgument(0)).when(peers).verifyCourier(anyString(), nullable(String.class));
        doNothing().when(peers).validatePair(anyString(), anyString(), nullable(String.class));
        assertTrue(commands.enabled());
    }

    private JdbcTemplate creditDb() {
        return new JdbcTemplate(new DriverManagerDataSource(CREDIT_DB.getJdbcUrl(),
                CREDIT_DB.getUsername(), CREDIT_DB.getPassword()));
    }

    private String key() {
        return UUID.randomUUID().toString();
    }

    private String token(String actor) {
        Instant now = Instant.now();
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String claims = "{\"sub\":\"" + actor + "\",\"aud\":\"demo-foc\",\"iss\":\"https://securetoken.google.com/demo-foc\","
                + "\"iat\":" + now.getEpochSecond() + ",\"exp\":" + now.plusSeconds(3600).getEpochSecond() + "}";
        return "Bearer " + header + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    private String account() {
        String actor = key();
        RestClient.builder().baseUrl("http://" + CREDIT.getHost() + ":" + CREDIT.getMappedPort(8080)).build()
                .post().uri("/api/credits/registration-facts").header("Authorization", token(actor))
                .body(java.util.Map.of("eventId", key(), "userId", actor, "occurredAt", Instant.now().toString()))
                .retrieve().toBodilessEntity();
        return actor;
    }

    private CommandRequest input(String actor) {
        return new CommandRequest("CREATE", actor, null, 0, new CommandRequest.Creation("contract test",
                "pickup", "delivery", 5, 15, Instant.now().plusSeconds(7200), false, null, 0, 0, null));
    }

    private OrderResponse open(String actor) {
        CommandView result = commands.submit(key(), input(actor), token(actor));
        assertEquals("SUCCESS", result.outcome(), result.message());
        return result.result();
    }

    private void due(String key) {
        orderDb.update("update order_commands set next_retry_at=clock_timestamp()-interval '1 second' where command_id=?", key);
    }

    @Test
    void actualReservationIsHistoricallyReplayedWithOneLedgerEffect() {
        String actor = account();
        String key = key();
        CommandRequest input = input(actor);
        CommandView created = commands.submit(key, input, token(actor));
        assertEquals("SUCCESS", created.outcome(), created.message());
        assertEquals(created.orderId(), commands.submit(key, input, token(actor)).orderId());
        assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
        assertEquals("APPLIED", creditDb().queryForObject("select outcome from credit_command_records where command_id=?", String.class, key));
    }

    @Test
    void lostReservationResponseLeavesPendingThenFreshResumeFinishesSameCandidate() {
        String actor = account();
        String key = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost response after Credit commit"); })
                .doCallRealMethod().when(gateway).execute(eq(key), any(), anyLong(), anyString());
        CommandView pending = commands.submit(key, input(actor), token(actor));
        assertEquals("PENDING", pending.status());
        assertEquals("APPLIED", creditDb().queryForObject("select outcome from credit_command_records where command_id=?", String.class, key));
        assertEquals(0, orderDb.queryForObject("select count(*) from orders where id=?", Integer.class, pending.orderId()));
        due(key);
        scheduler.processDueOrders();
        assertEquals("AUTHORIZATION_REQUIRED", store.get(key).reason());
        CommandView completed = commands.resume(key, actor, token(actor));
        assertEquals("SUCCESS", completed.outcome(), completed.message());
        assertEquals(pending.orderId(), completed.orderId());
        assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
    }

    @Test
    void actualSocketTimeoutAfterCreditCommitRecoversSameKey() throws Exception {
        String actor = account();
        String key = key();
        try (LostReplyProxy proxy = new LostReplyProxy()) {
            HttpCreditCommandGateway timed = new HttpCreditCommandGateway(proxy.url(), Duration.ofSeconds(1), Duration.ofSeconds(1));
            doAnswer(call -> timed.execute(call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3)))
                    .doCallRealMethod().when(gateway).execute(eq(key), any(), anyLong(), anyString());
            CommandView pending = commands.submit(key, input(actor), token(actor));
            assertEquals("PENDING", pending.status());
            assertEquals("RETRY_SCHEDULED", pending.reason());
            assertEquals("APPLIED", creditDb().queryForObject("select outcome from credit_command_records where command_id=?", String.class, key));
            CommandView completed = commands.resume(key, actor, token(actor));
            assertEquals("SUCCESS", completed.outcome(), completed.message());
            assertEquals(pending.orderId(), completed.orderId());
            assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
        }
    }

    /** Forward to the actual provider, then withhold its committed PUT reply.
     * This exercises a real HTTP read timeout, not a fabricated financial result.
     */
    private static final class LostReplyProxy implements AutoCloseable {
        private final com.sun.net.httpserver.HttpServer server;
        private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
        private final CountDownLatch release = new CountDownLatch(1);

        LostReplyProxy() throws java.io.IOException {
            server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(workers);
            server.createContext("/", exchange -> {
                try {
                    java.net.http.HttpRequest.Builder request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                            "http://" + CREDIT.getHost() + ":" + CREDIT.getMappedPort(8080) + exchange.getRequestURI()))
                            .timeout(Duration.ofSeconds(10));
                    for (String name : java.util.List.of("Authorization", "Idempotency-Key", "X-Command-Generation", "Content-Type")) {
                        String value = exchange.getRequestHeaders().getFirst(name);
                        if (value != null) request.header(name, value);
                    }
                    request.method(exchange.getRequestMethod(), java.net.http.HttpRequest.BodyPublishers.ofByteArray(
                            exchange.getRequestBody().readAllBytes()));
                    java.net.http.HttpResponse<byte[]> response = java.net.http.HttpClient.newHttpClient()
                            .send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                    if ("PUT".equals(exchange.getRequestMethod()) && response.statusCode() == 201) {
                        release.await(15, TimeUnit.SECONDS);
                        return; // Never deliver the successful financial reply.
                    }
                    for (String name : java.util.List.of("Idempotency-Key", "Command-Outcome", "Content-Type")) {
                        response.headers().firstValue(name).ifPresent(value -> exchange.getResponseHeaders().set(name, value));
                    }
                    exchange.sendResponseHeaders(response.statusCode(), response.body().length == 0 ? -1 : response.body().length);
                    exchange.getResponseBody().write(response.body());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();
        }

        String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

        @Override public void close() {
            release.countDown();
            server.stop(0);
            workers.close();
        }
    }

    @Test
    void rollbackAfterCreditCommitIsRecoveredWithoutAnotherReservation() {
        String actor = account();
        String key = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("crash before Order commit"); })
                .doCallRealMethod().when(creation).createConfirmed(eq(key), anyString(), eq(actor), anyString(),
                        anyString(), anyString(), anyLong(), anyInt(), any(), nullable(sg.edu.nus.foc.order.domain.RepostPlan.class), anyString());
        CommandView pending = commands.submit(key, input(actor), token(actor));
        assertEquals("PENDING", pending.status());
        assertEquals(0, orderDb.queryForObject("select count(*) from orders where id=?", Integer.class, pending.orderId()));
        assertEquals("SUCCESS", commands.resume(key, actor, token(actor)).outcome());
        assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
    }

    @Test
    void expiredUnresolvedAcceptanceIsReversedBeforeOneExpiryRefundIntent() {
        String requester = account();
        String courier = account();
        OrderResponse opened = open(requester);
        String key = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost assignment response"); })
                .when(gateway).execute(eq(key), any(), anyLong(), anyString());
        CommandView pending = commands.submit(key,
                new CommandRequest("ACCEPT", courier, opened.getId(), opened.getVersion(), null), token(courier));
        assertEquals("PENDING", pending.status());
        assertThrows(OrderProblem.class, () -> transitions.cancel(key(), opened.getId(), requester, opened.getVersion(), token(requester)));
        orderDb.update("update orders set expires_at=clock_timestamp()-interval '1 second' where id=?", opened.getId());
        scheduler.processDueOrders();
        assertEquals("OPEN", orderDb.queryForObject("select status from orders where id=?", String.class, opened.getId()));
        assertEquals("REJECTED", commands.resume(key, courier, token(courier)).outcome());
        assertEquals("COMPENSATED", creditDb().queryForObject("select outcome from credit_command_records where command_id=?", String.class, key));
        assertNull(creditDb().queryForObject("select courier_id from credit_reservations where order_id=?", String.class, opened.getId()));
        scheduler.processDueOrders();
        scheduler.processDueOrders();
        assertEquals("EXPIRED", orderDb.queryForObject("select status from orders where id=?", String.class, opened.getId()));
        assertEquals(1, orderDb.queryForObject("select count(*) from order_event_outbox where order_id=? and event_type='OpenOrderRefundTaskEvent'",
                Integer.class, opened.getId()));
    }

    @Test
    void abortLostResponseKeepsAcceptedUntilOneHistoryAndResetAreConfirmed() {
        String requester = account();
        String courier = account();
        OrderResponse opened = open(requester);
        OrderResponse accepted = commands.submit(key(), new CommandRequest("ACCEPT", courier,
                opened.getId(), opened.getVersion(), null), token(courier)).result();
        assertNotNull(accepted);
        String abortKey = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost clear response"); })
                .doCallRealMethod().when(gateway).execute(eq(abortKey), any(), anyLong(), anyString());
        assertEquals("PENDING", commands.submit(abortKey,
                new CommandRequest("ABORT", courier, accepted.getId(), accepted.getVersion(), null), token(courier)).status());
        assertThrows(OrderProblem.class, () -> transitions.start(key(), accepted.getId(), courier, accepted.getVersion(), token(courier)));
        assertEquals(0, orderDb.queryForObject("select count(*) from order_courier_attempts where order_id=?", Integer.class, accepted.getId()));
        assertEquals("SUCCESS", commands.resume(abortKey, courier, token(courier)).outcome());
        assertEquals(1, orderDb.queryForObject("select count(*) from order_courier_attempts where order_id=?", Integer.class, accepted.getId()));
        assertNull(creditDb().queryForObject("select courier_id from credit_reservations where order_id=?", String.class, accepted.getId()));
        OrderResponse reopened = commands.status(abortKey, courier, token(courier)).result();
        commands.submit(key(), new CommandRequest("ACCEPT", courier, reopened.getId(), reopened.getVersion(), null), token(courier));
        gateway.execute(abortKey, new sg.edu.nus.foc.order.application.recovery.CreditCommandGateway.Mutation(
                "ABORT", reopened.getId(), courier, 0), 99, token(courier));
        assertEquals(courier, creditDb().queryForObject("select courier_id from credit_reservations where order_id=?", String.class, reopened.getId()));
    }

    @Test
    void simultaneousSameKeyUsesOneClaimAndRealReservation() throws Exception {
        String actor = account();
        String key = key();
        CommandRequest input = input(actor);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertTrue(release.await(15, TimeUnit.SECONDS)); return call.callRealMethod(); })
                .when(gateway).execute(eq(key), any(), anyLong(), anyString());
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<CommandView> worker = pool.submit(() -> commands.submit(key, input, token(actor)));
            assertTrue(entered.await(15, TimeUnit.SECONDS));
            try {
                assertEquals("PENDING", commands.submit(key, input, token(actor)).status());
            } finally {
                release.countDown();
            }
            assertEquals("SUCCESS", worker.get(15, TimeUnit.SECONDS).outcome());
        }
        verify(gateway, times(1)).execute(eq(key), any(), anyLong(), anyString());
        assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
    }

    @Test
    void confirmedInsufficientCreditsIsTerminalWithoutAnOrder() {
        String actor = account();
        CommandRequest.Creation fields = input(actor).creation();
        CommandRequest excessive = new CommandRequest("CREATE", actor, null, 0,
                new CommandRequest.Creation(fields.description(), fields.pickup(), fields.delivery(), 51,
                        fields.duration(), fields.expiresAt(), false, null, 0, 0, null));
        String key = key();
        CommandView rejected = commands.submit(key, excessive, token(actor));
        assertEquals("REJECTED", rejected.outcome(), rejected.message());
        assertEquals("INSUFFICIENT_CREDITS", rejected.reason());
        assertEquals("REJECTED", creditDb().queryForObject("select outcome from credit_command_records where command_id=?", String.class, key));
        assertEquals(0, orderDb.queryForObject("select count(*) from orders where id=?", Integer.class, rejected.orderId()));
    }

    @Test
    void providerRestartKeepsCommittedOutcomeAndSameCandidate() {
        String actor = account();
        String key = key();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("lost response before provider restart"); })
                .doCallRealMethod().when(gateway).execute(eq(key), any(), anyLong(), anyString());
        CommandView pending = commands.submit(key, input(actor), token(actor));
        CREDIT.getDockerClient().restartContainerCmd(CREDIT.getContainerId()).withTimeout(1).exec();
        try {
            Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)).waitUntilReady(CREDIT);
        } catch (RuntimeException failure) {
            System.err.println(CREDIT.getLogs());
            throw failure;
        }
        CommandView completed = commands.resume(key, actor, token(actor));
        assertEquals("SUCCESS", completed.outcome(), completed.message());
        assertEquals(pending.orderId(), completed.orderId());
        assertEquals(1, creditDb().queryForObject("select count(*) from credit_ledger where command_id=?", Integer.class, key));
    }
}
