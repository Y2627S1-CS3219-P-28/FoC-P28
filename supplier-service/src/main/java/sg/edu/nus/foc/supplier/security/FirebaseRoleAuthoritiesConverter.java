package sg.edu.nus.foc.supplier.security;

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns a verified Firebase ID token into Spring authorities by asking the User Service for the
 * user's roles. Roles are never read from client-controlled data.
 */
public class FirebaseRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final RoleProvider roleProvider;

    public FirebaseRoleAuthoritiesConverter(RoleProvider roleProvider) {
        this.roleProvider = roleProvider;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return roleProvider.rolesFor(jwt.getSubject(), jwt.getClaimAsString("email")).stream()
                .sorted()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.authority()))
                .toList();
    }
}
