package com.p28.userservice.logic;

import java.util.List;

public class UserRoleContext {
    private String userId;
    private List<String> roles;

    public UserRoleContext(String userId, List<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    // getters and setters
    public String getUserId() {
        return this.userId;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public void setUserId(String newUserId) {
        this.userId = newUserId;
        return;
    }

    public void setRoles(List<String> newRoles) {
        this.roles = newRoles;
        return;
    }
}