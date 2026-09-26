package com.p28.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.p28.userservice.logic.AddUserRequest;
import com.p28.userservice.logic.CourierElgibility;
import com.p28.userservice.logic.UpdateUserRequest;
import com.p28.userservice.logic.UserRoleContext;
import com.p28.userservice.logic.UserService;
import com.p28.userservice.logic.UserSummary;
import com.p28.userservice.model.User;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<User>> fetchAllUsers() {
        return ResponseEntity.ok(userService.fetchAllUsers());
    }

    // GET /api/users/:userId
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(
            @PathVariable String userId) {

        User user = userService.getUserById(userId);

        return ResponseEntity.ok(user);
    }

    // GET /api/users/profile/:userId
    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfileInfo(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userService.getProfileInfo(userId)
        );
    }

    // GET /api/users/role-context/:userId
    @GetMapping("/role-context/{userId}")
    public ResponseEntity<UserRoleContext> getRoleContext(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userService.getRoleContext(userId)
        );
    }


    // GET /api/users/courier-eligibility/:userId
    @GetMapping("/courier-eligibility/{userId}")
    public ResponseEntity<CourierElgibility> getCourierEligibility(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userService.getCourierEligibility(userId)
        );
    }


    // GET /api/users/summary/:userId
    @GetMapping("/summary/{userId}")
    public ResponseEntity<UserSummary> getUserSummary(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userService.getUserSummary(userId)
        );
    }

    // POST /api/users/
    // Only accepts username, email and firebaseUid
    @PostMapping
    public ResponseEntity<User> addUser(
            @RequestBody AddUserRequest request) {

        User user = userService.addUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // PUT routes for order outcomes
    @PutMapping("/{courierId}/outcome-completed")
    public ResponseEntity<User> acceptCourierOutcomeCompleted(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeCompleted(courierId)
        );
    }

    // PUT /api/users/:id
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {

        User user = userService.updateUser(id, request);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/{courierId}/outcome-aborted")
    public ResponseEntity<User> acceptCourierOutcomeAborted(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeAborted(courierId)
        );
    }

    @PutMapping("/{courierId}/outcome-overdue")
    public ResponseEntity<User> acceptCourierOutcomeOverdue(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeOverdue(courierId)
        );
    }

    // DELETE /api/users/:id
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable String id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("User removed");
    }
}

