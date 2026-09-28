package sg.edu.nus.foc.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(PostgresTestConfiguration.class)
@AutoConfigureMockMvc
class OpenApiDocumentationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    @Test
    void everyEndpointIsDocumented() throws Exception {
        String json = mvc.perform(get("/api/orders/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var document = mapper.readTree(json);
        assertThat(document.path("openapi").asString()).startsWith("3.");
        // This setup has no business controllers yet. Do not invent an API to fill the spec.
        for (var path : document.path("paths").properties()) {
            assertThat(path.getKey()).startsWith("/api/orders");
            for (var operation : path.getValue().properties()) {
                if (!Set.of("get", "post", "put", "patch", "delete").contains(operation.getKey())) continue;
                assertThat(operation.getValue().path("summary").asString("")).isNotBlank();
                assertThat(operation.getValue().path("responses").propertyNames())
                        .anyMatch(code -> code.startsWith("2"));
            }
        }
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/openapi.json"), json);
    }

    @Test
    void missingEndpointIsNotInventedByDocumentation() throws Exception {
        mvc.perform(get("/api/orders/not-implemented")).andExpect(status().isNotFound());
    }
}
