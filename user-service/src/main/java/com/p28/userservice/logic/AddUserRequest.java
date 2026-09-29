package com.p28.userservice.logic;

public class AddUserRequest {
    private String userId;
    private String email;

    public AddUserRequest() {}

    public AddUserRequest(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    // Getter
    public String getUserId() {
        return this.userId;
    }

    public String getEmail() {
        return this.email;
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
}