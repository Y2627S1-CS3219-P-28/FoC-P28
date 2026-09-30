package sg.edu.nus.foc.order.application;

public interface UserServicePort {
    void verifyRequester(String userId, String authorization);
    void verifyCourier(String userId, String authorization);
}
