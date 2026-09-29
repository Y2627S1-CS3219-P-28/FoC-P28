package sg.edu.nus.foc.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=prod")
@Import(PostgresTestConfiguration.class)
@AutoConfigureMockMvc
class OrderServiceApplicationTests {
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;

	@Test
	void contextLoads() {
		assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class))
				.isEqualTo(2);
	}

	@Test
	void databaseBackedReadinessIsAvailable() throws Exception {
		mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
	}

	@Test
	void productionDoesNotExposeApiDocsOrEnvironment() throws Exception {
		mvc.perform(get("/api/orders/errands")).andExpect(status().isNotFound());
		mvc.perform(get("/api/orders/v3/api-docs")).andExpect(status().isNotFound());
		mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
	}

}
