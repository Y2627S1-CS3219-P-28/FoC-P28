/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-25
 * Mode: Test generation and testing assistance.
 * Scope: Generated Firestore emulator test infrastructure for the Credit Service integration tests.
 * Author review: I reviewed for correctness.
 */
package sg.edu.nus.foc.credit.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

public final class FirestoreEmulator {

    public static final String PROJECT_ID = "demo-foc";

    private static final FirestoreEmulatorContainer CONTAINER = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"));

    static {
        CONTAINER.start();
    }

    private FirestoreEmulator() {
    }

    public static String endpoint() {
        return CONTAINER.getEmulatorEndpoint();
    }

    public static Firestore client(String databaseId) {
        return FirestoreOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setDatabaseId(databaseId)
                .setEmulatorHost(endpoint())
                .build()
                .getService();
    }

    public static void clear(String databaseId) {
        URI uri = URI.create("http://" + endpoint() + "/emulator/v1/projects/" + PROJECT_ID + "/databases/"
                + databaseId + "/documents");
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = http.send(HttpRequest.newBuilder(uri).DELETE().build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Emulator reset failed: HTTP " + response.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Emulator reset failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted resetting emulator", e);
        }
    }
}
