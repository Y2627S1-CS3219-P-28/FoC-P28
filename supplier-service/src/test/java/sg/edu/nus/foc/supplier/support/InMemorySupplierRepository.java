package sg.edu.nus.foc.supplier.support;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import sg.edu.nus.foc.supplier.supplier.DuplicateSupplierException;
import sg.edu.nus.foc.supplier.supplier.Supplier;
import sg.edu.nus.foc.supplier.supplier.SupplierDetails;
import sg.edu.nus.foc.supplier.supplier.SupplierNotFoundException;
import sg.edu.nus.foc.supplier.supplier.SupplierRepository;
import sg.edu.nus.foc.supplier.supplier.SupplierSource;

/** Test double with the same uniqueness rules as the Firestore repository. */
public class InMemorySupplierRepository implements SupplierRepository {

    private final Map<String, Supplier> suppliers = new LinkedHashMap<>();
    private final Clock clock;
    private int sequence;
    public int findAllCalls;

    public InMemorySupplierRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<Supplier> findAll() {
        findAllCalls++;
        return new ArrayList<>(suppliers.values());
    }

    @Override
    public Optional<Supplier> findById(String id) {
        return Optional.ofNullable(suppliers.get(id));
    }

    @Override
    public List<Supplier> findAllById(Collection<String> ids) {
        return ids.stream().distinct().map(suppliers::get).filter(Objects::nonNull).toList();
    }

    @Override
    public Optional<Supplier> findByNaturalKey(String naturalKey) {
        return suppliers.values().stream().filter(s -> s.details().naturalKey().equals(naturalKey)).findFirst();
    }

    @Override
    public Supplier create(SupplierDetails details, SupplierSource source) {
        findByNaturalKey(details.naturalKey()).ifPresent(s -> {
            throw new DuplicateSupplierException(s.id());
        });
        Instant now = clock.instant();
        Supplier supplier = new Supplier("s" + (++sequence), details, true, source, now, now);
        suppliers.put(supplier.id(), supplier);
        return supplier;
    }

    @Override
    public Supplier update(String id, SupplierDetails details, boolean active) {
        Supplier existing = findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        findByNaturalKey(details.naturalKey()).filter(s -> !s.id().equals(id)).ifPresent(s -> {
            throw new DuplicateSupplierException(s.id());
        });
        Supplier updated = new Supplier(id, details, active, existing.source(), existing.createdAt(), clock.instant());
        suppliers.put(id, updated);
        return updated;
    }
}
