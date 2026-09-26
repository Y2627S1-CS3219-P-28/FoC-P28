package com.p28.userservice.logic;

import java.util.List;
import java.util.Date;

public class UserSummary {
    private String username;
    private String userId;
    private String email;
    private List<String> roles;
    private int penalty;
    private boolean isCourierSuspended = false;
    private Date suspensionEndDate;

    public UserSummary(
        String username,
        String userId, 
        String email,
        List<String> roles,
        int penalty,
        boolean isCourierSuspended,
        Date suspensionEndDate) {

        this.username = username;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
        this.penalty = penalty;
        this.isCourierSuspended = isCourierSuspended;
        this.suspensionEndDate = suspensionEndDate;
    }

    // getters
    public String getUsername() {
        return this.username;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getEmail() {
        return this.email;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public int getPenalty() {
        return this.penalty;
    }

    public boolean getIsCourierSuspended() {
        return this.isCourierSuspended;
    }

    public Date getSuspensionEndDate() {
        return this.suspensionEndDate;
    }
}
