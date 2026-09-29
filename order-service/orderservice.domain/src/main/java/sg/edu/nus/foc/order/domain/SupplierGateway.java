package sg.edu.nus.foc.order.domain;

import java.util.List;

public interface SupplierGateway {
    record Supplier(String id, String name, String location) {}

    List<Supplier> list();

    Supplier resolve(String id);

    void validatePair(String pickup, String delivery);
}
