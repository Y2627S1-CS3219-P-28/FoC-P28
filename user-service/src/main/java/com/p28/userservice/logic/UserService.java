package com.p28.userservice.logic;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.p28.userservice.model.User;
import com.p28.userservice.repository.UserRepository;
import com.p28.userservice.authentication.FirebaseAuthService;

@Service
public class UserService {
    private final FirebaseAuthService firebaseAuthService;
    private final UserRepository userRepository;
    private final CreditServiceClient creditServiceClient;

    public UserService(UserRepository userRepository, 
            FirebaseAuthService firebaseAuthService,
            CreditServiceClient creditServiceClient) {
        this.userRepository = userRepository;
        this.firebaseAuthService = firebaseAuthService;
        this.creditServiceClient = creditServiceClient;
    }

    public List<User> fetchAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByUserId(String userId) {
        return userRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    public User getProfileInfo(String userId) {
        return userRepository
                .findByUserId(userId)
                .orElseThrow(() -> 
                        new RuntimeException("User not found"));
    }

    public UserRoleContext getRoleContext(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() -> 
                        new RuntimeException("User not found"));

        return new UserRoleContext(user.getUserId(), user.getRoles());
    }

    public CourierElgibility getCourierEligibility(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() -> 
                        new RuntimeException("User not found"));

        Instant now = Instant.now();

        return new CourierElgibility(
                !(user.getPenalty() >= 10
                && user.getSuspensionEndDate() != null
                && user.getSuspensionEndDate().isAfter(now))
            );
    }

    public UserSummary getUserSummary(String userId) {
        User user = getUserByUserId(userId);

        return new UserSummary(
            user.getUsername(), 
            user.getUserId(), 
            user.getEmail(), 
            user.getRoles(), 
            user.getPenalty(), 
            user.getIsCourierSuspended(), 
            user.getSuspensionEndDate());
    }

    public User addUser(AddUserRequest request, String token) {
        if (request.getUserId() == null ||
            request.getEmail() == null) {

            throw new IllegalArgumentException("Please enter all fields.");
        }

        User user = new User();

        List<String> roles = new ArrayList<String>();
        roles.add("courier");
        roles.add("requester");

        user.setUserId(request.getUserId());
        user.setEmail(request.getEmail());
        user.setUsername("");
        user.setRoles(roles);

        try {
            UUID eventId = UUID.randomUUID();

            User savedUser = userRepository.save(user);

            // Add initial credits
            creditServiceClient.registerUser(
                token,
                savedUser.getUserId(),
                eventId);

            return savedUser;
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    // Currently, users can only update their email and username
    // TODO: update password
    public User updateUser(String userId, UpdateUserRequest request) {
        User user = getUserByUserId(userId);

        String email = request.getEmail();
        String username = request.getUsername();

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            firebaseAuthService.updateUserEmail(userId, email);
        }

        if (username != null) {
            user.setUsername(username);
        }

        return userRepository.save(user);
    }

    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }

    // public User acceptCourierOutcomeCompleted(String courierId) {
       // User courier = getUserByUserId(courierId);

       // adjustPenalty(courier, -1);

       // return userRepository.save(courier);
    // }

    // public User acceptCourierOutcomeAborted(String courierId) {
       // User courier = getUserByUserId(courierId);

        // adjustPenalty(courier, 2);

        // return userRepository.save(courier);
    // }

    public void verifyIdentity(Object accessContext) {
        // TODO: implement
    }

    // Single method to update a user's penalty
    // Accepts negative and positive penalty.
    public User adjustPenalty(String userId, int penalty) {
        User user = getUserByUserId(userId);
        int newPenalty = user.getPenalty() + penalty;
        System.out.println(String.format("Penalty is %d", newPenalty));

        if (newPenalty < 0) {
            newPenalty = 0;
        }

        if (newPenalty >= 10) {
            newPenalty = 10;

            // Applies suspension
            System.out.println(String.format("Applying suspension to user %s", user.getEmail()));

            List<String> roles = user.getRoles();
            roles.remove("courier");
            user.setRoles(roles);
            user.setIsCourierSuspended(true);
            user.setSuspensionEndDate(Instant.now().plus(1, ChronoUnit.MONTHS));
        }

        user.setPenalty(newPenalty);

        return userRepository.save(user);
    }

    private User refreshSuspensionStatus(User user) {
        if (user.getIsCourierSuspended()
                && user.getSuspensionEndDate() != null
                && Instant.now().isAfter(user.getSuspensionEndDate())) {

            user.setIsCourierSuspended(false);
            user.setSuspensionEndDate(null);
            List<String> roles = user.getRoles();
            roles.add("courier");
            user.setRoles(roles);

            return userRepository.save(user);
        }

        return user;
    }
}
