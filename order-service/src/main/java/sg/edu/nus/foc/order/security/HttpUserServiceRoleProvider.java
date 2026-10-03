package sg.edu.nus.foc.order.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Retrieves the token owner's roles from User Service using the verified caller token. */
public class HttpUserServiceRoleProvider implements RoleProvider {
    private final RestClient client;

    public HttpUserServiceRoleProvider(RestClient client) {
        this.client = client;
    }

    record RoleContextResponse(String userId, List<String> roles) {
    }

    @Override
    public Set<Role> rolesFor(Jwt caller) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        RoleContextResponse response;
        try {
            response = client.get()
                    .uri("/api/users/role-context")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + caller.getTokenValue())
                    .retrieve()
                    .body(RoleContextResponse.class);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                return roles;
            }
            throw new RoleLookupException("User Service role lookup failed for " + caller.getSubject(), exception);
        } catch (RestClientException exception) {
            throw new RoleLookupException("User Service role lookup failed for " + caller.getSubject(), exception);
        }
        if (response != null && response.roles() != null) {
            response.roles().forEach(value -> Role.fromId(value).ifPresent(roles::add));
        }
        return roles;
    }
}
