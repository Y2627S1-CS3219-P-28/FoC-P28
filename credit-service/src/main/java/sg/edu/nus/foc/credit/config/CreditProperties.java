/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Boilerplate generation.
 * Scope: Generated Spring Boot configuration code for team-finalized runtime and OpenAPI settings.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("foc.credit")
public record CreditProperties(AuthSettings auth, List<String> corsOrigins) {

    public CreditProperties {
        auth = auth != null ? auth : new AuthSettings(null, null);
        corsOrigins = corsOrigins != null ? List.copyOf(corsOrigins) : List.of();
    }

    public record AuthSettings(String projectId, String emulatorHost) {
        public AuthSettings {
            projectId = blankToDefault(projectId, "demo-foc");
            emulatorHost = emulatorHost != null ? emulatorHost.trim() : "";
        }

        public boolean usesEmulator() {
            return !emulatorHost.isEmpty();
        }
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
