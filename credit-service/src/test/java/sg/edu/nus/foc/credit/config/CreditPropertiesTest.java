/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial configuration tests for team-finalized runtime settings.
 * Author review:  I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CreditPropertiesTest {

    @Test
    void appliesSafeDefaults() {
        CreditProperties properties = new CreditProperties(null, null);
        assertThat(properties.auth().projectId()).isEqualTo("demo-foc");
        assertThat(properties.auth().usesEmulator()).isFalse();
        assertThat(properties.corsOrigins()).isEmpty();
    }

    @Test
    void trimsConfigurationAndCopiesOrigins() {
        List<String> origins = new java.util.ArrayList<>(List.of("https://example.com"));
        CreditProperties properties = new CreditProperties(
                new CreditProperties.AuthSettings(" auth-project ", " localhost:9099 "), origins);
        origins.clear();
        assertThat(properties.auth().projectId()).isEqualTo("auth-project");
        assertThat(properties.auth().usesEmulator()).isTrue();
        assertThat(properties.corsOrigins()).containsExactly("https://example.com");
    }
}
