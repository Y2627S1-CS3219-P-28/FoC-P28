package sg.edu.nus.foc.order.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.foc.order.api.mapper.OrderMapperImpl;
import sg.edu.nus.foc.order.application.OrderQueryService;
import sg.edu.nus.foc.order.config.SecurityConfiguration;
import sg.edu.nus.foc.order.config.ProductionMethodSecurityConfiguration;
import sg.edu.nus.foc.order.domain.repository.OrderPage;
import sg.edu.nus.foc.order.security.RoleProvider;

@WebMvcTest(controllers = AdminOrderController.class)
@ActiveProfiles("prod")
@Import({SecurityConfiguration.class, ProductionMethodSecurityConfiguration.class, OrderMapperImpl.class})
class AdminOrderControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService queries;

    @MockitoBean
    private RoleProvider roleProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerUiWebjarAssetsArePublic() throws Exception {
        mockMvc.perform(get("/webjars/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedNonAdminIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders").with(user("requester").roles("REQUESTER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseOneBasedPageAndOptionalStatus() throws Exception {
        when(queries.allOrders(null, 1, 10)).thenReturn(new OrderPage(List.of(), 1, 10, 11, 2));

        mockMvc.perform(get("/api/orders")
                        .param("page", "2")
                        .param("size", "10")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(11));

        verify(queries).allOrders(null, 1, 10);
    }

    @Test
    void statusFilterIsBoundAndLargePageSizeIsCapped() throws Exception {
        when(queries.allOrders(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new OrderPage(List.of(), 0, 100, 0, 0));

        mockMvc.perform(get("/api/orders")
                        .param("status", "COMPLETED")
                        .param("size", "500")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        verify(queries).allOrders(org.mockito.ArgumentMatchers.eq(sg.edu.nus.foc.order.domain.OrderStatus.COMPLETED),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(100));
    }
}
