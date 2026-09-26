package sg.edu.nus.foc.supplier.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the User Service role endpoint (User backlog F6.1):
 * {@code GET {USER_SERVICE_URL}/api/users/{uid}/roles} returning {@code {"roles": ["requester", ...]}}.
 * Unknown role names are ignored so the User Service can add roles without breaking this service.
 */
public class HttpUserServiceRoleProvider implements RoleProvider {

    private final RestClient client;

    public HttpUserServiceRoleProvider(RestClient client) {
        this.client = client;
    }

    record RolesResponse(List<String> roles) {
    }

    @Override
    public Set<Role> rolesFor(String uid, String email) {
        RolesResponse response;
        try {
            response = client.get().uri("/api/users/{uid}/roles", uid).retrieve().body(RolesResponse.class);
        } catch (RestClientException e) {
            throw new RoleLookupException("User Service role lookup failed for " + uid, e);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        if (response != null && response.roles() != null) {
            response.roles().forEach(value -> Role.fromId(value).ifPresent(roles::add));
        }
        return roles;
    }
}
