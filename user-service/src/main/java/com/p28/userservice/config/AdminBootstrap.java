package com.p28.userservice.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.p28.userservice.model.User;
import com.p28.userservice.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${admin.bootstrap-enabled:false}")
    private boolean enabled;

    @Value("${admin.bootstrap-email:}")
    private String bootstrapEmail;

    public AdminBootstrap(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Executes on user service startup
    // Have to create Firebase account first through console
    // Then a MongoDB user is created
    @Override
    public void run(String... args) {

        // Bootstrap is disabled
        if (!enabled) {
            return;
        }

        // Admin already exists
        if (userRepository.findFirstByRolesContaining("admin").isPresent()) {
            System.out.println("Admin already exists. Skipping bootstrap.");
            return;
        }

        if (bootstrapEmail.isBlank()) {
            throw new IllegalStateException(
                "Admin bootstrap is enabled but no email was configured."
            );
        }

        try {
            // Find the Firebase Auth account
            UserRecord firebaseUser =
                FirebaseAuth.getInstance()
                    .getUserByEmail(bootstrapEmail);

            // Create MongoDB user
            User user = new User();

            user.setUserId(firebaseUser.getUid());
            user.setEmail(firebaseUser.getEmail());
            user.setUsername("");

            List<String> roles = new ArrayList<>();
            roles.add("admin");
            roles.add("courier");
            roles.add("requester");

            user.setRoles(roles);

            user.setPenalty(0);
            user.setIsCourierSuspended(false);

            userRepository.save(user);

            System.out.println(
                "Admin bootstrap successful: " + bootstrapEmail
            );

        } catch (FirebaseAuthException e) {
            throw new RuntimeException(
                "Could not find bootstrap admin in Firebase: "
                + bootstrapEmail,
                e
            );
        }
    }
}