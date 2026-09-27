package com.p28.userservice.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

    public FirebaseConfig() {
        if (FirebaseApp.getApps().isEmpty()) {

            String projectId = System.getenv("FIREBASE_AUTH_PROJECT_ID");
            String emulatorHost = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");

            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setProjectId(projectId);

            if (emulatorHost != null && !emulatorHost.isBlank()) {
                // Local development:
                // Firebase Auth is running in the emulator,
                // so real Google credentials are not required.
                GoogleCredentials credentials =
                        GoogleCredentials.create(
                                new AccessToken("fake-token", null)
                        );
                builder.setCredentials(credentials);
            } else {
                // Production:
                // Use real Google Application Default Credentials.
                try {
                    builder.setCredentials(
                            GoogleCredentials.getApplicationDefault()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Could not load Firebase production credentials",
                            e
                    );
                }
            }
            FirebaseApp.initializeApp(builder.build());
        }
    }
}