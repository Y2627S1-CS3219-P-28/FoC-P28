package sg.edu.nus.foc.supplier.security;

import java.util.Set;

/** Resolves a signed-in user's roles from the User Service (the source of truth for roles). */
public interface RoleProvider {

    /**
     * @param uid   Firebase user ID (the token's {@code sub})
     * @param email may be null
     * @throws RoleLookupException if the User Service cannot be reached or answers unexpectedly
     */
    Set<Role> rolesFor(String uid, String email);
}
