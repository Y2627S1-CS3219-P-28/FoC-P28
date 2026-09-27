package com.p28.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

import com.google.firebase.auth.FirebaseToken;

import com.p28.userservice.authentication.AuthenticationService;
import com.p28.userservice.authentication.FirebaseAuthService;
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
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public UserController(
            UserService userService, 
            AuthenticationService authenticationService) {

        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    // Test
    // @ExceptionHandler(HttpMessageNotReadableException.class)
    // public ResponseEntity<String> handleHttpMessageNotReadable(
            // HttpMessageNotReadableException e) {

        // e.printStackTrace();

        // return ResponseEntity.badRequest().body(
            // e.getMostSpecificCause() != null
                // ? e.getMostSpecificCause().toString()
                // : e.toString()
        // );
    // }

    // GET /api/users
    @Operation(summary = "Get all users")
    @GetMapping
    public ResponseEntity<List<User>> fetchAllUsers() {
        return ResponseEntity.ok(userService.fetchAllUsers());
    }

    // GET /api/users/:firebaseUid
    @Operation(summary = "Get specific user based on firebaseUid")
    @GetMapping("/{firebaseUid}")
    public ResponseEntity<User> getUser(
            @PathVariable String firebaseUid) {

        User user = userService.getUserById(firebaseUid);

        return ResponseEntity.ok(user);
    }

    // GET /api/users/role-context/:firebaseUid
    // Used for checking if a user is of a certain role for certain actions
    @Operation(summary = "Get user roles from auth token")
    @GetMapping("/role-context")
    public ResponseEntity<UserRoleContext> getRoleContext(
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        String firebaseUid = token.getUid();

        return ResponseEntity.ok(
                userService.getRoleContext(firebaseUid)
        );
    }


    // GET /api/users/courier-eligibility/:firebaseUid
    @Operation(summary = "Get courier eligibility from Firebase uid")
    @GetMapping("/courier-eligibility/{firebaseUid}")
    public ResponseEntity<CourierElgibility> getCourierEligibility(
            @PathVariable String firebaseUid) {

        return ResponseEntity.ok(
                userService.getCourierEligibility(firebaseUid)
        );
    }


    // GET /api/users/summary/:firebaseUid
    @Operation(summary = "Get user summary from firebase Uid")
    @GetMapping("/summary/{firebaseUid}")
    public ResponseEntity<UserSummary> getUserSummary(
            @PathVariable String firebaseUid) {

        return ResponseEntity.ok(
                userService.getUserSummary(firebaseUid)
        );
    }

    // GET /api/users/me
    @Operation(summary = "Get user's own information from auth token")
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(
            @RequestHeader("Authorization") String authorizationHeader) {
        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        String firebaseUid = token.getUid();

        User user = userService.getUserByFirebaseUid(firebaseUid);

        return ResponseEntity.ok(user);
    }

    // POST /api/users/
    // Only accepts username, email and firebaseUid
    @Operation(summary = "Add user into database")
    @PostMapping
    public ResponseEntity<User> addUser(
            @RequestBody AddUserRequest request) {

        User user = userService.addUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // PUT /api/users/:id
    @Operation(summary = "Update user info")
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {

        User user = userService.updateUser(id, request);

        return ResponseEntity.ok(user);
    }

    // PUT routes for order outcomes
    @Operation(summary = "Update user penalty from normal completion")
    @PutMapping("/{courierId}/outcome-completed")
    public ResponseEntity<User> acceptCourierOutcomeCompleted(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeCompleted(courierId)
        );
    }

    @Operation(summary = "Update user penalty from normal completion")
    @PutMapping("/{courierId}/outcome-aborted")
    public ResponseEntity<User> acceptCourierOutcomeAborted(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeAborted(courierId)
        );
    }

    @Operation(summary = "Update user penalty from normal completion")
    @PutMapping("/{courierId}/outcome-overdue")
    public ResponseEntity<User> acceptCourierOutcomeOverdue(
            @PathVariable String courierId) {

        return ResponseEntity.ok(
            userService.acceptCourierOutcomeOverdue(courierId)
        );
    }

    // DELETE /api/users/:id
    @Operation(summary = "Delete specified user from database")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable String id) {

        userService.deleteUser(id);

        return ResponseEntity.ok("User removed");
    }
}

