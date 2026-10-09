package com.p28.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import com.google.firebase.auth.FirebaseToken;

import com.p28.userservice.authentication.AuthenticationService;
import com.p28.userservice.authentication.FirebaseAuthService;
import com.p28.userservice.logic.AddUserRequest;
import com.p28.userservice.logic.ApplyPenaltyRequest;
import com.p28.userservice.logic.CourierEligibility;
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

    // GET /api/users/:userId
    // Only admins can get full info of a specific user
    @Operation(summary = "Get specific user based on userId")
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorizationHeader) {

        User user = userService.getUserByUserId(userId);

        return ResponseEntity.ok(user);
    }

    // GET /api/users/role-context/
    // Used for checking if a user is of a certain role for certain actions
    @Operation(summary = "Get requesting user's roles from auth token")
    @GetMapping("/role-context")
    public ResponseEntity<UserRoleContext> getRoleContext(
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);
        String userId = token.getUid();

        return ResponseEntity.ok(
                userService.getRoleContext(userId)
        );
    }


    // GET /api/users/courier-eligibility/
    @Operation(summary = "Get requesting user's courier eligibility from auth token")
    @GetMapping("/courier-eligibility")
    public ResponseEntity<CourierEligibility> getCourierEligibility(
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);
        String userId = token.getUid();

        return ResponseEntity.ok(
                userService.getCourierEligibility(userId)
        );
    }


    // GET /api/users/summary/:userId
    @Operation(summary = "Get user summary from user id")
    @GetMapping("/summary/{userId}")
    public ResponseEntity<UserSummary> getUserSummary(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                userService.getUserSummary(userId)
        );
    }

    // GET /api/users/me
    @Operation(summary = "Get requesting user's own information from auth token")
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(
            @RequestHeader("Authorization") String authorizationHeader) {
        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        // Treat Firebase uid as user id
        String userId = token.getUid();

        User user = userService.getUserByUserId(userId);

        return ResponseEntity.ok(user);
    }

    // POST /api/users/
    // Only accepts username, email and userId
    @Operation(summary = "Add user into database")
    @PostMapping
    public ResponseEntity<User> addUser(
            @RequestHeader("Authorization") String authorizationHeader) {
        
        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        // after authentication, we know this token is correct to pass to credit service
        String tokenToPass = authorizationHeader.substring(7);

        String userId = token.getUid();
        String email = token.getEmail();

        User user = userService.addUser(tokenToPass, userId, email);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // PUT /api/users/me
    // Update own info
    @Operation(summary = "Update requesting user's own info")
    @PutMapping("/me")
    public ResponseEntity<User> updateUser(
            @RequestBody UpdateUserRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        // Get user from auth token
        FirebaseToken token = authenticationService.authenticate(authorizationHeader);
        String userId = token.getUid();
        User user = userService.updateUser(userId, request);

        return ResponseEntity.ok(user);
    }

    // PUT /api/users/:userId
    // Only admin can update other users' info
    @Operation(summary = "Update any user's info")
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(
            @PathVariable String userId,
            @RequestBody UpdateUserRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        // Check if requester is admin
        FirebaseToken token = authenticationService.authenticate(authorizationHeader);
        String requesterId = token.getUid();
        User requester = userService.getUserByUserId(requesterId);
        if (!userService.verifyAdmin(requester)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "USER_NOT_ADMIN"
            );
        }
            
        User user = userService.updateUser(userId, request);

        return ResponseEntity.ok(user);
    }

    // PUT /api/users/:userId/admin
    // Only admin can update other users' info
    @PutMapping("/{userId}/admin")
    public ResponseEntity<User> promoteToAdmin(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        User requester = userService.getUserByUserId(token.getUid());

        if (!userService.verifyAdmin(requester)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "USER_NOT_ADMIN"
            );
        }

        User promotedUser = userService.promoteToAdmin(userId);

        return ResponseEntity.ok(promotedUser);
    }

    // PUT /api/users/{userId}/penalty
    // Only used by other services
    @Operation(summary = "Update any user's penalty")
    @PutMapping("/{userId}/penalty")
    public ResponseEntity<User> updateUser(
            @PathVariable String userId,
            @RequestBody ApplyPenaltyRequest request) {
            
        User user = userService.adjustPenalty(userId, request.getPenalty());

        return ResponseEntity.ok(user);
    }

    // DELETE /api/users/me
    // User can delete their own account
    @Operation(summary = "Delete user themselves from database")
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteUser(
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        String userId = token.getUid();

        userService.deleteUser(userId);

        return ResponseEntity.ok("User deleted");
    }

    // DELETE /api/users/:id
    // Only admins can delete other users
    @Operation(summary = "Delete specified user from database")
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUserAsAdmin(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorizationHeader) {

        FirebaseToken token = authenticationService.authenticate(authorizationHeader);

        User requester = userService.getUserByUserId(token.getUid());

        if (!userService.verifyAdmin(requester)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "USER_NOT_ADMIN"
            );
        }
        
        userService.deleteUser(userId);

        return ResponseEntity.ok("User removed");
    }
}

