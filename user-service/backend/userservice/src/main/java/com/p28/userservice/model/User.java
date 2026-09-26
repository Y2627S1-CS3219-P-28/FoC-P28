package com.p28.userservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.sql.Date;
import java.util.List;

@Document("users")
public class User {

    @Id
    private String id;

    private String username;
    private String userId;
    private String firebaseUid;

    @Indexed(unique = true)
    private String email;

    private List<String> roles;
    private int penalty = 0;
    private boolean isCourierSuspended = false;
    private Date suspensionEndDate;

    public User() {
    }

    // Getters
    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUserId() {
        return this.userId;
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

    // Setters
    public void setId(String newId) {
        this.id = newId;
        return;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
        return;
    }

    public void setUserId(String newUserId) {
        this.userId = newUserId;
        return;
    }

    public void setFirebaseUid(String newFirebaseUid) {
        this.firebaseUid = newFirebaseUid;
        return;
    }

    public void setEmail(String newEmail) {
        this.email = newEmail;
        return;
    }

    public void setRoles(List<String> newRoles) {
        this.roles = newRoles;
        return;
    }

    public void setPenalty(int newPenalty) {
        this.penalty = newPenalty;
        return;
    }

    public void setIsCourierSuspended(boolean isCourierSuspended) {
        this.isCourierSuspended = isCourierSuspended;
        return;
    }

    public void setSuspensionEndDate(Date newSuspensionEndDate) {
        this.suspensionEndDate = newSuspensionEndDate;
        return;
    }
}