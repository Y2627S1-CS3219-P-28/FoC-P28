package com.p28.userservice.authentication;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FirebaseAuthService {

    public FirebaseToken verifyToken(String idToken) {

        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);

        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication token"
            );
        }
    }
}