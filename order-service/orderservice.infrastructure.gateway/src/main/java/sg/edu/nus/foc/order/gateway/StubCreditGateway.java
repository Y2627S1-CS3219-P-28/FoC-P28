package sg.edu.nus.foc.order.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import sg.edu.nus.foc.order.domain.CreditGateway;

/** Development-only success responses. Does not read or modify any credit balance. */
@Component
@Profile("!prod & (local | test)")
public class StubCreditGateway implements CreditGateway {
    @Override
    public boolean reserve(String errandId, String requesterId, long amount) {
        return true;
    }

    @Override
    public boolean release(String errandId, String requesterId) {
        return true;
    }
}
