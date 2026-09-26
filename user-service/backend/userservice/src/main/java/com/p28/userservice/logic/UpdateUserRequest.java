package com.p28.userservice.logic;

public class UpdateUserRequest {
    private String userId;
    private String email;
    private String username;

    public UpdateUserRequest(String userId, String email, String username) {
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
    public void setUserId(String userId) {
        this.userId = userId;
        return;
    }

    public void setEmail(String email) {
        this.email = email;
        return;
    }

    public void setUsername(String username) {
        this.username = username;
        return;
    }
}
