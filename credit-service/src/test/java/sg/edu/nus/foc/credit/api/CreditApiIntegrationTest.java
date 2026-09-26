/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated initial API integration and OpenAPI verification tests for team-finalized behavior.
 * Author review: I reviewed for correctness, edited where needed, and added boundary cases.
 */
package sg.edu.nus.foc.credit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.foc.credit.support.FirestoreEmulator;

@SpringBootTest
@AutoConfigureMockMvc
class CreditApiIntegrationTest {

    private static final String DATABASE = "credit-api-test";
    private static final String USER = "firebase-user-123";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("foc.credit.firestore.emulator-host", FirestoreEmulator::endpoint);
        registry.add("foc.credit.firestore.database-id", () -> DATABASE);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper mapper;

    @BeforeEach
    void reset() {
        FirestoreEmulator.clear(DATABASE);
    }

    @Test
    void registrationCreatesExactlyFiftyCreditsAndReplaysAsOk() throws Exception {
        RegistrationFactRequest request = registration(USER);
        mvc.perform(post("/api/credits/registration-facts")
                        .with(jwt().jwt(token -> token.subject(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER))
                .andExpect(jsonPath("$.totalBalance").value(50))
                .andExpect(jsonPath("$.reservedBalance").value(0))
                .andExpect(jsonPath("$.usableBalance").value(50));

        mvc.perform(post("/api/credits/registration-facts")
                        .with(jwt().jwt(token -> token.subject(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(50));
    }

    @Test
    void registrationRequiresAuthenticationOwnershipAndValidInput() throws Exception {
        byte[] body = mapper.writeValueAsBytes(registration(USER));
        mvc.perform(post("/api/credits/registration-facts")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
        mvc.perform(post("/api/credits/registration-facts")
                        .with(jwt().jwt(token -> token.subject("someone-else")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(post("/api/credits/registration-facts")
                        .with(jwt().jwt(token -> token.subject(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"\",\"occurredAt\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private RegistrationFactRequest registration(String userId) {
        return new RegistrationFactRequest(UUID.randomUUID(), userId, Instant.parse("2026-09-25T08:00:00Z"));
    }

    private void register(String userId) throws Exception {
        mvc.perform(post("/api/credits/registration-facts")
                        .with(jwt().jwt(token -> token.subject(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(registration(userId))))
                .andExpect(status().isCreated());
    }

}
