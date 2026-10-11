package sg.edu.nus.foc.order.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.foc.order.api.mapper.OrderMapperImpl;
import sg.edu.nus.foc.order.application.OrderQueryService;
import sg.edu.nus.foc.order.config.SecurityConfiguration;
import sg.edu.nus.foc.order.domain.repository.OrderPage;

@WebMvcTest(controllers = AdminOrderController.class)
@ActiveProfiles("local")
@Import({SecurityConfiguration.class, OrderMapperImpl.class})
class AdminOrderControllerLocalSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService queries;

    @Test
    void localAdminOrderEndpointDoesNotRequireAuthenticationOrAdminRole() throws Exception {
        when(queries.allOrders(null, 0, 20)).thenReturn(new OrderPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }
}
