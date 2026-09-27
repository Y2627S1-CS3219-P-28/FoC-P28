package com.p28.userservice.logic;

public class AddUserRequest {
    private String firebaseUid;
    private String email;

    public AddUserRequest() {}

    public AddUserRequest(String firebaseUid, String email) {
        this.firebaseUid = firebaseUid;
        this.email = email;
    }

    // Getter
    public String getFirebaseUid() {
        return this.firebaseUid;
    }

    public String getEmail() {
        return this.email;
    }

    // Setter
    public void setFirebaseUid(String newFirebaseUid) {
        this.firebaseUid = newFirebaseUid;
        return;
    }

    public void setEmail(String newEmail) {
        this.email = newEmail;
        return;
    }
}