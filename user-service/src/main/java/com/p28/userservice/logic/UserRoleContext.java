package com.p28.userservice.logic;

import java.util.List;

public class UserRoleContext {
    private String firebaseUid;
    private List<String> roles;

    public UserRoleContext() {}

    public UserRoleContext(String firebaseUid, List<String> roles) {
        this.firebaseUid = firebaseUid;
        this.roles = roles;
    }

    // getters and setters
    public String getFirebaseUid() {
        return this.firebaseUid;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public void setFirebaseUid(String newFirebaseUid) {
        this.firebaseUid = newFirebaseUid;
        return;
    }

    public void setRoles(List<String> newRoles) {
        this.roles = newRoles;
        return;
    }
}