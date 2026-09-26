package com.p28.userservice.logic;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import com.p28.userservice.model.User;
import com.p28.userservice.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> fetchAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> 
                        new RuntimeException("User not found"));
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

        Date now = new Date();

        return new CourierElgibility(
                !(user.getPenalty() >= 10
                && user.getSuspensionEndDate() != null
                && user.getSuspensionEndDate().after(now))
            );
    }

    public UserSummary getUserSummary(String userId) {
        User user = userRepository
                .findByUserId(userId)
                .orElseThrow(() -> 
                        new RuntimeException("User not found"));

        return new UserSummary(
            user.getUsername(), 
            user.getUserId(), 
            user.getEmail(), 
            user.getRoles(), 
            user.getPenalty(), 
            user.getIsCourierSuspended(), 
            user.getSuspensionEndDate());
    }

    public User addUser(AddUserRequest request) {
        if (request.getUserId() == null ||
            request.getEmail() == null ||
            request.getUsername() == null) {

            throw new IllegalArgumentException("Please enter all fields.");
        }

        User user = new User();

        List<String> roles = new ArrayList<String>();
        roles.add("courier");
        roles.add("requester");

        user.setUserId(request.getUserId());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setRoles(roles);

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    public User updateUser(String id, UpdateUserRequest request) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() -> 
                    new RuntimeException("User not found"));

        String email = request.getEmail();
        String username = request.getUsername();

        if (email != null) {
            user.setEmail(email);
        }

        if (username != null) {
            user.setUsername(username);
        }

        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }

    public User acceptCourierOutcomeCompleted(String courierId) {
        User courier = userRepository
                .findByUserId(courierId)
                .orElseThrow(() -> 
                        new RuntimeException("Courier not found"));

        int newPenalty = courier.getPenalty() - 2;

        if (newPenalty < 0) {
            newPenalty = 0;
        }

        courier.setPenalty(newPenalty);

        return userRepository.save(courier);
    }

    public User acceptCourierOutcomeOverdue(String courierId) {
        User courier = userRepository
                .findByUserId(courierId)
                .orElseThrow(() -> 
                        new RuntimeException("Courier not found"));

        int newPenalty = courier.getPenalty() + 1;

        if (newPenalty >= 10) {
            newPenalty = 10;

            // TODO: apply suspension
        }

        courier.setPenalty(newPenalty);

        return userRepository.save(courier);
    }

    public User acceptCourierOutcomeAborted(String courierId) {
        User courier = userRepository.findByUserId(courierId)
                .orElseThrow(() -> new RuntimeException("Courier not found"));

        int newPenalty = courier.getPenalty() + 2;

        if (newPenalty >= 10) {
            newPenalty = 10;

            // TODO: apply suspension
        }

        courier.setPenalty(newPenalty);

        return userRepository.save(courier);
    }

    public void verifyIdentity(Object accessContext) {
        // TODO: implement
    }
}
