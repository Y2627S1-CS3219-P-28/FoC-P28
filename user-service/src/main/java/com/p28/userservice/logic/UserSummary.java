package com.p28.userservice.logic;

import java.util.List;
import java.util.Date;

public class UserSummary {
    private String username;
    private String firebaseUid;
    private String email;
    private List<String> roles;
    private int penalty;
    private boolean isCourierSuspended = false;
    private Date suspensionEndDate;

    public UserSummary() {}

    public UserSummary(
        String username,
        String firebaseUid, 
        String email,
        List<String> roles,
        int penalty,
        boolean isCourierSuspended,
        Date suspensionEndDate) {

        this.username = username;
        this.firebaseUid = firebaseUid;
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

    public String getFirebaseUid() {
        return this.firebaseUid;
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
