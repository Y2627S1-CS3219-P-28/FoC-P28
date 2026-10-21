package sg.edu.foc.userservice.repository;

import java.util.Optional;

import sg.edu.foc.userservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);
}