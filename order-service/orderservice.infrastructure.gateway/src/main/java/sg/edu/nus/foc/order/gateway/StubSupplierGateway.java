package sg.edu.nus.foc.order.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import sg.edu.nus.foc.order.domain.*;

import java.util.List;

/** Sample catalogue only. Real Supplier Service remains authoritative outside this prototype. */
@Component
@Profile("!prod & (local | test)")
public class StubSupplierGateway implements SupplierGateway {
    private static final List<Supplier> FIXTURES =
            List.of(
                    new Supplier("demo-canteen", "Campus canteen (demo)", "Campus dining area"),
                    new Supplier("demo-library", "Library (demo)", "Library entrance"),
                    new Supplier("demo-residence", "Student residence (demo)", "Residence lobby"));

    public List<Supplier> list() {
        return FIXTURES;
    }

    public Supplier resolve(String id) {
        return FIXTURES.stream()
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElseThrow(
                        () ->
                                new OrderProblem(
                                        "VALIDATION_ERROR",
                                        "Choose a supplier from the sample catalogue.",
                                        List.of(
                                                new OrderProblem.Detail(
                                                        "supplierId",
                                                        "Unknown sample supplier."))));
    }

    public void validatePair(String pickup, String delivery) {
        resolve(pickup);
        resolve(delivery);
        if (pickup.equals(delivery))
            throw new OrderProblem("VALIDATION_ERROR", "Pickup and delivery must be different.");
    }
}
