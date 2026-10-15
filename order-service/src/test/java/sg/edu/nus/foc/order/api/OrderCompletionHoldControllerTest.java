package sg.edu.nus.foc.order.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sg.edu.nus.foc.order.api.mapper.OrderMapperImpl;
import sg.edu.nus.foc.order.application.OrderCompletionHoldService;
import sg.edu.nus.foc.order.config.ProductionMethodSecurityConfiguration;
import sg.edu.nus.foc.order.config.SecurityConfiguration;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.security.Role;
import sg.edu.nus.foc.order.security.RoleProvider;

@WebMvcTest(controllers = OrderCompletionHoldController.class)
@ActiveProfiles("prod")
@Import({SecurityConfiguration.class, ProductionMethodSecurityConfiguration.class, OrderMapperImpl.class})
class OrderCompletionHoldControllerTest {

    private static final String PATH = "/api/orders/internal/order-1/completion-holds/report-1";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderCompletionHoldService holds;

    @MockitoBean
    private RoleProvider roles;

    @MockitoBean
    private JwtDecoder decoder;

    @Test
    void requiresAuthenticationAndRequesterRole() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"requesterId\":\"owner\"}"))
                .andExpect(status().isUnauthorized());
        for (String role : new String[] {"COURIER", "ADMIN"}) {
            mvc.perform(put(PATH).with(user("owner").roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requesterId\":\"owner\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void forwardsFirebaseTokenAndUsesTheExplicitBooleanWireName() throws Exception {
        Jwt token = Jwt.withTokenValue("token").header("alg", "RS256").subject("owner").build();
        when(decoder.decode("token")).thenReturn(token);
        when(roles.rolesFor(token)).thenReturn(Set.of(Role.REQUESTER));
        Instant now = Instant.parse("2026-10-14T00:00:00Z");
        Order order = Order.open("owner", "books", "p", "d", 5, 30, now, now.plusSeconds(3600));
        order.placeCompletionHold("owner");
        when(holds.place("order-1", "report-1", "owner", "Bearer token")).thenReturn(order);

        mvc.perform(put(PATH).header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterId\":\"owner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("report-1"))
                .andExpect(jsonPath("$.isCompletionHold").value(true))
                .andExpect(jsonPath("$.completionHold").doesNotExist());
        verify(holds).place("order-1", "report-1", "owner", "Bearer token");
    }

    @Test
    void rejectsInvalidPayloadAndOwnerMismatch() throws Exception {
        mvc.perform(put(PATH).with(user("owner").roles("REQUESTER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        when(holds.place("order-1", "report-1", "other", null))
                .thenThrow(OrderProblem.forbidden("Only the requester may place a report hold on their order."));
        mvc.perform(put(PATH).with(user("other").roles("REQUESTER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requesterId\":\"other\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsBindingConflictAndHasNoReleaseOperation() throws Exception {
        when(holds.place("order-1", "report-1", "owner", null))
                .thenThrow(OrderProblem.conflict("The report hold conflicts with an existing placement."));
        mvc.perform(put(PATH).with(user("owner").roles("REQUESTER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requesterId\":\"owner\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
        mvc.perform(delete(PATH).with(user("owner").roles("REQUESTER")))
                .andExpect(status().isMethodNotAllowed());
    }
}
