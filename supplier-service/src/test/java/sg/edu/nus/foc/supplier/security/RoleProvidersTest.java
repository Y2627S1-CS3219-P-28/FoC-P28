package sg.edu.nus.foc.supplier.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RoleProvidersTest {

    @Test
    void mockGivesEveryoneRequesterAndCourierAndAdminsByEmail() {
        MockUserServiceRoleProvider provider = new MockUserServiceRoleProvider(List.of(" Admin@U.NUS.edu ", ""));
        assertThat(provider.rolesFor("u1", "student@u.nus.edu")).containsExactlyInAnyOrder(Role.REQUESTER, Role.COURIER);
        assertThat(provider.rolesFor("u2", "admin@u.nus.edu")).contains(Role.ADMIN);
        assertThat(provider.rolesFor("u3", null)).doesNotContain(Role.ADMIN);
    }

    @Test
    void httpProviderMapsKnownRolesAndIgnoresUnknownOnes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://user-service/api/users/u1/roles"))
                .andRespond(withSuccess("{\"roles\":[\"requester\",\"ADMIN\",\"superhero\"]}", MediaType.APPLICATION_JSON));

        HttpUserServiceRoleProvider provider = new HttpUserServiceRoleProvider(builder.build());
        assertThat(provider.rolesFor("u1", null)).containsExactlyInAnyOrder(Role.REQUESTER, Role.ADMIN);
        server.verify();
    }

    @Test
    void httpProviderHandlesEmptyBodiesAndFailures() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://user-service/api/users/u1/roles"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://user-service/api/users/u2/roles")).andRespond(withServerError());

        HttpUserServiceRoleProvider provider = new HttpUserServiceRoleProvider(builder.build());
        assertThat(provider.rolesFor("u1", null)).isEmpty();
        assertThatThrownBy(() -> provider.rolesFor("u2", null)).isInstanceOf(RoleLookupException.class);
    }

    @Test
    void roleIdsRoundTrip() {
        assertThat(Role.fromId(" courier ")).contains(Role.COURIER);
        assertThat(Role.fromId("nope")).isEmpty();
        assertThat(Role.fromId(null)).isEmpty();
        assertThat(Role.ADMIN.id()).isEqualTo("admin");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }
}
