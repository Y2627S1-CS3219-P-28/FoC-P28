package sg.edu.nus.foc.order.security;

import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class FirebaseRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final RoleProvider roleProvider;

    public FirebaseRoleAuthoritiesConverter(RoleProvider roleProvider) {
        this.roleProvider = roleProvider;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        try {
            return roleProvider.rolesFor(jwt).stream()
                    .sorted()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.authority()))
                    .toList();
        } catch (RoleLookupException exception) {
            throw new AuthenticationServiceException("User Service role lookup is unavailable.", exception);
        }
    }
}
