package com.p28.userservice.logic;

public class UpdateUserRequest {
    private String firebaseUid;
    private String email;
    private String username;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String firebaseUid, String email, String username) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.username = username;
    }

    // Getter
    public String getFirebaseUid() {
        return this.firebaseUid;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUsername() {
        return this.username;
    }

    // Setter
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
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
