package sg.edu.nus.foc.user.repository;

import java.util.Optional;

import sg.edu.nus.foc.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);

    Optional<User> findFirstByRolesContaining(String role);

    long countByRolesContaining(String role);
}