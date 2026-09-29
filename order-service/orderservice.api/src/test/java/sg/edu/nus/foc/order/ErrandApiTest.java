package sg.edu.nus.foc.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import(PostgresTestConfiguration.class)
@AutoConfigureMockMvc
class ErrandApiTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    String command() {
        return UUID.randomUUID().toString();
    }

    Map<String, Object> posting(String command) {
        return Map.of(
                "commandId",
                command,
                "requesterId",
                "requester-a",
                "description",
                "Collect my lunch please",
                "pickupSupplierId",
                "demo-canteen",
                "deliverySupplierId",
                "demo-library",
                "creditAmount",
                5,
                "deliveryDurationMinutes",
                30,
                "expiresAt",
                Instant.now().plusSeconds(7200).toString());
    }

    JsonNode post(String path, Object body, int expected) throws Exception {
        return json.readTree(
                mvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .post(path)
                                        .contentType("application/json")
                                        .content(json.writeValueAsString(body)))
                        .andExpect(status().is(expected))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    JsonNode create() throws Exception {
        return post("/api/orders/errands", posting(command()), 201);
    }

    Map<String, Object> action(String actor, long version, String cmd) {
        return Map.of("actorId", actor, "expectedVersion", version, "commandId", cmd);
    }

    @Test
    void sixSequencesPersistAndRetriesDoNotDuplicateCheckpoints() throws Exception {
        Map<String, Object> body = posting(command());
        JsonNode errand = post("/api/orders/errands", body, 201);
        String id = errand.path("id").asString();
        assertThat(errand.path("status").asString()).isEqualTo("OPEN");
        assertThat(post("/api/orders/errands", body, 201).path("id").asString()).isEqualTo(id);
        mvc.perform(get("/api/orders/errands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        post("/api/orders/errands/" + id + "/accept", action("requester-a", 0, command()), 403);
        Map<String, Object> accept = action("courier-b", 0, command());
        JsonNode order = post("/api/orders/errands/" + id + "/accept", accept, 200);
        String orderId = order.path("id").asString();
        assertThat(orderId).isNotEqualTo(id);
        assertThat(post("/api/orders/errands/" + id + "/accept", accept, 200).path("id").asString())
                .isEqualTo(orderId);
        String base = "/api/orders/executions/" + orderId;
        post(base + "/pickup", action("courier-b", 0, command()), 409);
        post(base + "/start", action("someone-else", 0, command()), 403);
        post(base + "/start", action("courier-b", 9, command()), 409);
        Map<String, Object> start = action("courier-b", 0, command());
        order = post(base + "/start", start, 200);
        String startedAt = order.path("startedAt").asString();
        assertThat(post(base + "/start", start, 200).path("startedAt").asString())
                .isEqualTo(startedAt);
        order =
                post(
                        base + "/pickup",
                        action("courier-b", order.path("version").asLong(), command()),
                        200);
        Map<String, Object> deliver =
                action("courier-b", order.path("version").asLong(), command());
        order = post(base + "/deliver", deliver, 200);
        assertThat(order.path("status").asString()).isEqualTo("DELIVERED");
        assertThat(order.path("checkpoints").size()).isEqualTo(4);
        assertThat(order.path("checkpoints").get(2).path("supplierId").asString())
                .isEqualTo("demo-canteen");
        assertThat(order.path("checkpoints").get(3).path("supplierId").asString())
                .isEqualTo("demo-library");
        assertThat(post(base + "/deliver", deliver, 200).path("deliveredAt").asString())
                .isEqualTo(order.path("deliveredAt").asString());
        mvc.perform(get(base))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkpoints.length()").value(4));
        mvc.perform(get("/api/orders/errands/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    void rejectsInvalidFieldsAndConflictingCommandReuse() throws Exception {
        JsonNode invalid =
                post("/api/orders/errands", Map.of("description", "bad", "creditAmount", 0), 400);
        assertThat(invalid.path("details").size()).isGreaterThan(3);
        Map<String, Object> body = new java.util.HashMap<>(posting(command()));
        post("/api/orders/errands", body, 201);
        body.put("creditAmount", 9);
        post("/api/orders/errands", body, 409);
        body.put("commandId", command());
        body.put("deliverySupplierId", "demo-canteen");
        post("/api/orders/errands", body, 400);
        body.put("deliverySupplierId", "missing");
        post("/api/orders/errands", body, 400);
        body.put("deliverySupplierId", "demo-library");
        body.put("expiresAt", Instant.now().toString());
        post("/api/orders/errands", body, 400);
        mvc.perform(get("/api/orders/errands?page=0&size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders/executions/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/prototype/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    @Test
    void unavailableListingsAndMalformedCommandsAreRejected() throws Exception {
        JsonNode errand = create();
        String id = errand.path("id").asString();
        jdbc.update(
                "update errands set expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                id);
        post("/api/orders/errands/" + id + "/accept", action("courier-b", 0, command()), 409);
        String result =
                mvc.perform(get("/api/orders/errands?size=100"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(result).doesNotContain(id);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/orders/errands")
                                .contentType("application/json")
                                .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/orders/errands?page=not-a-number"))
                .andExpect(status().isBadRequest());
        post("/api/orders/errands/missing/accept", action("courier-b", 0, command()), 404);
    }
}
