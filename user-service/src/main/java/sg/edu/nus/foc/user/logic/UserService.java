package sg.edu.nus.foc.user.logic;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import sg.edu.nus.foc.user.model.AccountStatus;
import sg.edu.nus.foc.user.model.OutboxEvent;
import sg.edu.nus.foc.user.model.User;
import sg.edu.nus.foc.user.repository.OutboxEventRepository;
import sg.edu.nus.foc.user.repository.UserRepository;
import sg.edu.nus.foc.user.authentication.FirebaseAuthService;
import sg.edu.nus.foc.user.event.UserCreatedEvent;


@Service
public class UserService {
    @Value("${admin.bootstrap-email:}")
    private String bootstrapAdminEmail;

    private final FirebaseAuthService firebaseAuthService;
    private final UserRepository userRepository;
    private final OrderServiceClient orderServiceClient;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper objectMapper;

    private static final int MAX_PENALTY = 10;

    public UserService(UserRepository userRepository,
            FirebaseAuthService firebaseAuthService,
            OrderServiceClient orderServiceClient,
            OutboxEventRepository outboxEventRepository,
            JsonMapper objectMapper) {
        this.userRepository = userRepository;
        this.firebaseAuthService = firebaseAuthService;
        this.orderServiceClient = orderServiceClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
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

    public CourierEligibility getCourierEligibility(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Instant now = Instant.now();

        return new CourierEligibility(verifyCourier(user) &&
                !(user.getPenalty() >= 10
                && user.getSuspensionEndDate() != null
                && user.getSuspensionEndDate().isAfter(now))
            );
    }

    public RequesterEligibility getRequesterEligibility(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return new RequesterEligibility(verifyRequester(user));
    }

    public AdminEligibility getAdminEligibility(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return new AdminEligibility(verifyAdmin(user));
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

    // Transactional so adding user and user-created event is done atomically
    @Transactional
    public User addUser(String userId, String email) {
        return createUser(userId, email, List.of("courier", "requester"));
    }

    @Transactional
    public User addAdmin(String userId, String email) {
        return createUser(userId, email, List.of("courier", "requester", "admin"));
    }

    public User createUser(String userId, String email, List<String> roles) {
        if (userId == null || userId.isBlank()
            || email == null || email.isBlank()) {
        throw new IllegalArgumentException(
                "User ID and email are required.");
        }

        Optional<User> existingUser = userRepository.findFirstByEmailAndStatus(email, AccountStatus.ACTIVE);

        if (existingUser.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setUsername("");
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        String eventId = UUID.randomUUID().toString();

        UserCreatedEvent event = new UserCreatedEvent(
                eventId,
                savedUser.getUserId(),
                savedUser.getEmail(),
                Instant.now(),
                1
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            outboxEventRepository.save(new OutboxEvent(
                    eventId,
                    "UserCreated",
                    payload
            ));
        } catch (JacksonException  e) {
            throw new IllegalStateException(
                    "Could not serialize UserCreated event.", e);
        }

        return savedUser;
    }

    // Currently, users can only update their username
    // Password and email are handled elsewhere
    public User updateUser(String userId, UpdateUserRequest request) {
        User user = getUserByUserId(userId);

        String username = request.getUsername();

        if (username != null) {
            user.setUsername(username);
        }

        return userRepository.save(user);
    }

    // Syncs MongoDB user email with Firebase
    public User syncVerifiedEmail(String userId) {
        User user = getUserByUserId(userId);

        // Firebase is the source of truth for the verified email.
        String verifiedEmail =
                firebaseAuthService.getVerifiedEmail(userId);

        // Update MongoDB only after Firebase verification succeeds.
        user.setEmail(verifiedEmail);

        return userRepository.save(user);
    }

    // Updates email
    public User updateUserEmail(String userId, UpdateUserRequest request) {
        User user = getUserByUserId(userId);
        String email = request.getEmail();

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            firebaseAuthService.updateUserEmail(userId, email);
        }

        return userRepository.save(user);
    }

    /*public void deleteUser(String token, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has pending orders
        // Throws error if it has
        boolean hasActiveOrders = orderServiceClient.hasActiveInvolvement(userId, token);
        if (hasActiveOrders) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete user with active orders"
            );
        }

        // Checks if it is the last admin
        // Throws error if it is
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("admin");
        if (isAdmin) {
            long adminCount = userRepository.countByRolesContaining("admin");

            if (adminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete the last administrator"
                );
            }
        }

        // Deletes user from Firebase
        firebaseAuthService.deleteUser(user.getUserId());

        // Deletes user from MongoDB
        userRepository.delete(user);
    }*/

    public void deactivateUser(String token, String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                    ));

        // Already deactivated.
        if (user.getStatus() == AccountStatus.DEACTIVATED) {
            return;
        }

        // Do not start another deletion while one is in progress.
        if (user.getStatus() == AccountStatus.DEACTIVATION_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account deactivation is already in progress"
            );
        }

        // Checks if it is the last admin
        // Throws error if it is
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("admin");
        if (isAdmin) {
            long adminCount = userRepository.countByRolesContainingAndStatus(
                        "admin",
                        AccountStatus.ACTIVE
                );

            if (adminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot disable the last administrator"
                );
            }
        }

        // Order owns the exact terminal-status rule and current requester/courier involvement.
        // Failures propagate before either database or Firebase is changed.
        if (!orderServiceClient.canDeleteUser(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot disable user with active orders"
            );
        }

        // Keep the MongoDB user document and mark it as undergoing deactivation.
        user.setStatus(AccountStatus.DEACTIVATION_PENDING);
        userRepository.save(user);

        // Delete the Firebase identity.
        try {
            firebaseAuthService.deleteUser(user.getUserId());

            // Firebase deletion confirmed.
            user.setStatus(AccountStatus.DEACTIVATED);
            userRepository.save(user);
        } catch (RuntimeException e) {
            // Leave the account pending if the Firebase deletion outcome is uncertain.
            // A recovery process can reconcile the state.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Account deletion could not be confirmed. Please try again later.",
                    e
            );
        }
    }

    // TODO: Implement this API and demoteAdmin
    // Adds admin role to user
    // Doesn't change role list if already admin
    public User promoteUser(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<String> roles = user.getRoles();
        if (!roles.contains("admin")) {
            roles.add("admin");
            user.setRoles(roles);
        }

        return userRepository.save(user);
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

        // Applies suspension
        if (newPenalty >= MAX_PENALTY) {
            newPenalty = MAX_PENALTY;

            // for debugging
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

    // Single method to suspend a user
    // Sets penalty to MAX_PENALTY
    public User suspendUser(String userId) {
        User user = getUserByUserId(userId);

        user.setPenalty(MAX_PENALTY);

        // for debugging
        System.out.println(String.format("Applying suspension to user %s", user.getEmail()));

        List<String> roles = user.getRoles();
        roles.remove("courier");
        user.setRoles(roles);
        user.setIsCourierSuspended(true);
        user.setSuspensionEndDate(Instant.now().plus(1, ChronoUnit.MONTHS));

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


    // Helper functions
    public boolean verifyAdmin(User user) {
        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("admin") && user.getStatus() == AccountStatus.ACTIVE);
    }

    public boolean verifyAdmin(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("admin") && user.getStatus() == AccountStatus.ACTIVE);
    }

    public boolean verifyRequester(User user) {
        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("requester") && user.getStatus() == AccountStatus.ACTIVE);
    }

    public boolean verifyRequester(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("requester") && user.getStatus() == AccountStatus.ACTIVE);
    }

    public boolean verifyCourier(User user) {
        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("courier") && user.getStatus() == AccountStatus.ACTIVE);
    }

    public boolean verifyCourier(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<String> roles = user.getRoles();
        return roles == null || (!roles.contains("courier") && user.getStatus() == AccountStatus.ACTIVE);
    }
}
