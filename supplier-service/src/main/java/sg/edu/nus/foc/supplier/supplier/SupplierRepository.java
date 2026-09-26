package sg.edu.nus.foc.supplier.supplier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence port for suppliers. Implementations must enforce name + building uniqueness atomically. */
public interface SupplierRepository {

    List<Supplier> findAll();

    Optional<Supplier> findById(String id);

    /** Returns the suppliers that exist, in no particular order; unknown IDs are skipped. */
    List<Supplier> findAllById(Collection<String> ids);

    Optional<Supplier> findByNaturalKey(String naturalKey);

    /** @throws DuplicateSupplierException if another supplier has the same name + building */
    Supplier create(SupplierDetails details, SupplierSource source);

    /**
     * Replaces the editable fields and active flag; the ID, source and creation time never change (F6.2.3).
     *
     * @throws SupplierNotFoundException  if {@code id} does not exist
     * @throws DuplicateSupplierException if the new name + building belongs to another supplier
     */
    Supplier update(String id, SupplierDetails details, boolean active);
}
