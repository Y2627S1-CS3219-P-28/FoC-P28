package sg.edu.nus.foc.order.application;

public interface UserServicePort {
    /**
     * Verifies the bearer token and returns the provider-confirmed user ID.
     * The caller must use this returned ID rather than trusting a request-body ID.
     */
    String verifyRequester(String userId, String authorization);

    /** Verifies courier eligibility and returns the provider-confirmed user ID. */
    String verifyCourier(String userId, String authorization);
}
