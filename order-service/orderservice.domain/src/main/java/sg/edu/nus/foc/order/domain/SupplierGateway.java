package sg.edu.nus.foc.order.domain;

import java.util.List;
import java.util.Optional;

public interface SupplierGateway {
    record Supplier(String id, String name, String location) {}

    List<Supplier> list();

    Optional<Supplier> resolve(String id);
}
