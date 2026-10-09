package com.p28.userservice.authentication;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FirebaseAuthService {

    public FirebaseToken verifyToken(String idToken) {

        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);

        } catch (FirebaseAuthException | IllegalArgumentException e) {
            System.err.println("Firebase token verification failed:");
            e.printStackTrace();
            
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication token"
            );
        }
    }

    public void updateUserEmail(String firebaseUid, String newEmail) {
        try {
            UpdateRequest request = new UpdateRequest(firebaseUid)
                    .setEmail(newEmail);

            FirebaseAuth.getInstance().updateUser(request);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to update Firebase email", e);
        }
    }

    public void deleteUser(String firebaseUid) {
        try {
            FirebaseAuth.getInstance().deleteUser(firebaseUid);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to delete Firebase user", e);
        }
    }
}