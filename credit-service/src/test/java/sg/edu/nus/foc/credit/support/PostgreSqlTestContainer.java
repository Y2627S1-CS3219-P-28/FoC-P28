/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-07
 * Mode: Boilerplate generation.
 * Scope: Generated PostgreSQL Testcontainers support based on the service integration-test requirements.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgreSqlTestContainer {

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
            DockerImageName.parse("postgres:15-alpine"));

    static {
        CONTAINER.withDatabaseName("credit_test");
        CONTAINER.withUsername("credit_test");
        CONTAINER.withPassword("credit_test");
        CONTAINER.start();
    }

    private PostgreSqlTestContainer() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
    }
}
