package sg.edu.nus.foc.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import sg.edu.nus.foc.order.application.LifecycleProcessingService;
import sg.edu.nus.foc.order.application.OrderAssignmentService;
import sg.edu.nus.foc.order.application.OrderCreationService;
import sg.edu.nus.foc.order.application.OrderQueryService;
import sg.edu.nus.foc.order.application.OrderRepostService;
import sg.edu.nus.foc.order.application.OrderTransitionService;
import sg.edu.nus.foc.order.application.UserServicePort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Repo-wide rule: every API endpoint is documented in OpenAPI.
 *
 * <p>This MVC slice intentionally mocks the application collaborators so the
 * documentation check does not require PostgreSQL, peer services, or Docker.
 */
@WebMvcTest(controllers = OrderController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({SpringDocWebMvcConfiguration.class, SwaggerConfig.class})
@TestPropertySource(properties = {
    "springdoc.api-docs.path=/api/orders/v3/api-docs",
    "springdoc.swagger-ui.path=/api/orders/docs",
    "order.lifecycle-token=test-lifecycle-token"
})
class OpenApiDocumentationTest {

    private static final String PREFIX = "/api/orders";
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");

    @MockitoBean private OrderCreationService creation;
    @MockitoBean private OrderAssignmentService assignment;
    @MockitoBean private OrderTransitionService transitions;
    @MockitoBean private OrderQueryService queries;
    @MockitoBean private OrderRepostService reposts;
    @MockitoBean private LifecycleProcessingService lifecycle;
    @MockitoBean private UserServicePort users;

    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper mapper;

    @Test
    void everyEndpointIsDocumented() throws Exception {
        String json = mvc.perform(get(PREFIX + "/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/openapi.json"), json);

        JsonNode paths = mapper.readTree(json).path("paths");
        assertThat(paths.isEmpty()).as("OpenAPI document lists no paths").isFalse();

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : paths.properties()) {
            if (!path.getKey().startsWith(PREFIX)) {
                problems.add(path.getKey() + ": outside " + PREFIX);
            }
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (!METHODS.contains(operation.getKey())) {
                    continue;
                }
                String where = operation.getKey().toUpperCase() + " " + path.getKey();
                if (operation.getValue().path("summary").asString("").isBlank()) {
                    problems.add(where + ": missing @Operation(summary = ...)");
                }
                boolean hasSuccess = operation.getValue().path("responses").propertyNames().stream()
                    .anyMatch(code -> code.startsWith("2"));
                if (!hasSuccess) {
                    problems.add(where + ": no 2xx response documented");
                }
            }
        }
        assertThat(problems).as("Undocumented API operations").isEmpty();
    }
}
