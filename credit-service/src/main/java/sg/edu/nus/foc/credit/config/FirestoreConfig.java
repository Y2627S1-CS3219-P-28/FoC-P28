/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Boilerplate generation.
 * Scope: Generated Spring Boot configuration code for team-finalized runtime, Firestore, and OpenAPI settings.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FirestoreConfig {

    @Bean(destroyMethod = "close")
    Firestore firestore(CreditProperties properties) {
        CreditProperties.FirestoreSettings settings = properties.firestore();
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setProjectId(settings.projectId())
                .setDatabaseId(settings.databaseId());
        if (!settings.emulatorHost().isEmpty()) {
            builder.setEmulatorHost(settings.emulatorHost());
        }
        return builder.build().getService();
    }
}
