package com.p28.userservice.logic;

public class AddUserRequest {
    private String userId;
    private String email;
    private String username;

    public AddUserRequest(String userId, String email, String username) {
        this.userId = userId;
        this.email = email;
        this.username = username;
    }

    // Getter
    public String getUserId() {
        return this.userId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUsername() {
        return this.username;
    }

    // Setter
    public void setUserId(String newUserId) {
        this.userId = newUserId;
        return;
    }

    public void setEmail(String newEmail) {
        this.email = newEmail;
        return;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
        return;
    }
}