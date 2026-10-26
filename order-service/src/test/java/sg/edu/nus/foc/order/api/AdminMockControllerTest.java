package sg.edu.nus.foc.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sg.edu.nus.foc.order.api.mapper.AdminMockMapper;
import sg.edu.nus.foc.order.api.mapper.AdminMockMapperImpl;
import sg.edu.nus.foc.order.application.adminmock.AdminMockService;

class AdminMockControllerTest {
    @Test
    void sampleListAppliesCalendarBoundsBeforePagingAndRejectsAnInvertedRange() throws Exception {
        mvc.perform(get("/api/orders/admin-mock/orders").param("createdFrom", "9999-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/orders/admin-mock/orders").param("createdBefore", "2000-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/orders/admin-mock/orders").param("orderId", "demo-order-002")
                .param("status", "ACCEPTED").param("createdFrom", "2000-01-01T00:00:00Z")
                .param("createdBefore", "9999-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/orders/admin-mock/orders").param("createdFrom", "2026-10-17T00:00:00Z")
                .param("createdBefore", "2026-10-16T00:00:00Z")).andExpect(status().isBadRequest());
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AdminMockService service = new AdminMockService(Mappers.getMapper(AdminMockMapper.class));
        mvc = MockMvcBuilders.standaloneSetup(new AdminMockController(service))
                .setControllerAdvice(new OrderExceptionHandler()).build();
    }

    @Test
    void sampleResolutionSavesReasonAndHighestPenaltyWithoutRealIntegrations() throws Exception {
        String path = "/api/orders/admin-mock/orders/demo-order-002/cancel";
        String body = """
                {"requesterId":"demo-user-001","courierId":"demo-user-002","appliedPenalties":["OVERDUE_DELIVERY","WRONG_LOCATION_DELIVERY"],"resolutionReason":"Delivery confirmed"}
                """;
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.adminPenaltyAmount").value(4))
                    .andExpect(jsonPath("$.adminResolutionReason").value("Delivery confirmed"));
        }
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body.replace("Delivery confirmed", "Other")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders/admin-mock/orders/demo-order-001/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"requesterId\":\"demo-user-001\",\"resolutionReason\":\"Requester requested cancellation\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.adminPenaltyDecision").value("WAIVED"));
    }

    @Test
    void sampleResolutionRejectsInvalidStatesParticipantsAndPolicies() throws Exception {
        String body = "{\"requesterId\":\"demo-user-001\",\"courierId\":\"demo-user-002\"}";
        for (String id : java.util.List.of("demo-order-006", "demo-order-007")) {
            mvc.perform(post("/api/orders/admin-mock/orders/" + id + "/cancel").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        mvc.perform(post("/api/orders/admin-mock/orders/missing/cancel").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/admin-mock/orders/demo-order-002/cancel").contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("}", ",\"appliedPenalties\":[\"SUCCESSFUL_DELIVERY\"]}"))).andExpect(status().isBadRequest());
    }

    @Test
    void userHistoryIsPaginatedAndScopedToTheSelectedSampleUser() throws Exception {
        mvc.perform(get("/api/orders/admin-mock/users/demo-user-002/transactions").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].transactionId").value("demo-user-002-transaction-3"))
                .andExpect(jsonPath("$.items[0].type").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.items[0].amount").value(10))
                .andExpect(jsonPath("$.items[0].occurredAt").isString())
                .andExpect(jsonPath("$.items[0].orderId").value("demo-order-006"));
        mvc.perform(get("/api/orders/admin-mock/users/demo-user-002/transactions")
                .param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("INITIAL_ALLOCATION"));
        mvc.perform(get("/api/orders/admin-mock/users/unknown/transactions"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/admin-mock/users/demo-user-002/transactions").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sampleRoleContextUsesTheCurrentIdentity() throws Exception {
        mvc.perform(get("/api/orders/admin-mock/role-context").param("userId", "signed-in-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("signed-in-user"))
                .andExpect(jsonPath("$.roles[0]").value("admin"));
        mvc.perform(get("/api/orders/admin-mock/role-context").param("userId", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordersFilterBeforePaginatingAndIncludeResolvedNames() throws Exception {
        mvc.perform(get("/api/orders/admin-mock/orders").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(24))
                .andExpect(jsonPath("$.totalPages").value(12))
                .andExpect(jsonPath("$.items[0].id").value("demo-order-001"))
                .andExpect(jsonPath("$.items[0].requesterName").value("Alex Tan"));
        mvc.perform(get("/api/orders/admin-mock/orders")
                .param("status", "COMPLETED").param("orderId", "demo-order-006"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/orders/admin-mock/orders").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userSearchDetailsAndMutationsUseSampleStateOnly() throws Exception {
        mvc.perform(get("/api/orders/admin-mock/users").param("q", "Alex").param("searchBy", "name"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/orders/admin-mock/users").param("q", "demo-user-002").param("searchBy", "id"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].username").value("Jamie Lee"));
        mvc.perform(post("/api/orders/admin-mock/users/demo-user-002/promote"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[2]").value("admin"));
        String future = Instant.now().plusSeconds(3600).toString();
        mvc.perform(post("/api/orders/admin-mock/users/demo-user-002/suspend")
                .contentType(MediaType.APPLICATION_JSON).content("{\"suspensionEndDate\":\"" + future + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isCourierSuspended").value(true));
        mvc.perform(post("/api/orders/admin-mock/users/demo-user-002/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"suspensionEndDate\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders/admin-mock/users/demo-user-002/deactivate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/orders/admin-mock/users/unknown")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/admin-mock/users").param("searchBy", "email"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supplierCreationAndSoftDeactivationMatchExistingWireFields() throws Exception {
        String supplier = """
                {"name":"Demo Printing","type":"Printing","building":"COM3","floor":"1",
                 "locationDescription":"Lobby","latitude":1.294,"longitude":103.773,
                 "openingTime":"09:00","closingTime":"18:00","imageUrl":""}
                """;
        String response = mvc.perform(post("/api/orders/admin-mock/suppliers")
                .contentType(MediaType.APPLICATION_JSON).content(supplier))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response).path("id").asText();
        mvc.perform(post("/api/orders/admin-mock/suppliers/" + id + "/deactivate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(post("/api/orders/admin-mock/suppliers")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders/admin-mock/suppliers").param("searchBy", "id").param("q", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/orders/admin-mock/suppliers/" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/orders/admin-mock/suppliers/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void mockBeansAreUnavailableInProduction() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(AdminMockController.class, AdminMockService.class, AdminMockMapperImpl.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AdminMockController.class);
                    assertThat(context).doesNotHaveBean(AdminMockService.class);
                });
    }

    @Test
    void optInAloneCannotExposeMocksInCloudOrWithoutAnEmulator() {
        new ApplicationContextRunner().withPropertyValues(
                "spring.profiles.active=prod", "order.admin-mock.enabled=true",
                "FOC_ENVIRONMENT=production", "order.security.auth.emulator-host=localhost:9099")
                .withUserConfiguration(AdminMockController.class, AdminMockService.class, AdminMockMapperImpl.class)
                .run(context -> assertThat(context).doesNotHaveBean(AdminMockController.class));
        new ApplicationContextRunner().withPropertyValues(
                "spring.profiles.active=prod", "order.admin-mock.enabled=true", "FOC_ENVIRONMENT=local")
                .withUserConfiguration(AdminMockController.class, AdminMockService.class, AdminMockMapperImpl.class)
                .run(context -> assertThat(context).doesNotHaveBean(AdminMockController.class));
        new ApplicationContextRunner().withPropertyValues(
                "spring.profiles.active=prod", "order.admin-mock.enabled=true", "FOC_ENVIRONMENT=local",
                "order.security.auth.emulator-host=localhost:9099")
                .withUserConfiguration(AdminMockController.class, AdminMockService.class, AdminMockMapperImpl.class)
                .run(context -> assertThat(context).hasSingleBean(AdminMockController.class));
    }
}
