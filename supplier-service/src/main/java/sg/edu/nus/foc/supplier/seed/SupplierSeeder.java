package sg.edu.nus.foc.supplier.seed;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.supplier.config.SupplierProperties;
import sg.edu.nus.foc.supplier.supplier.DuplicateSupplierException;
import sg.edu.nus.foc.supplier.supplier.Supplier;
import sg.edu.nus.foc.supplier.supplier.SupplierRepository;
import sg.edu.nus.foc.supplier.supplier.SupplierService;
import sg.edu.nus.foc.supplier.supplier.SupplierSource;

/**
 * Loads the supplier catalogue from {@code SUPPLIER_SEED_FILE} at startup (F2).
 *
 * <p>Runs once all beans exist but before the web server starts, so no request is served
 * before the catalogue is loaded (F2.2.1). A missing or malformed file aborts startup (F2.2.2).
 * The load is idempotent: re-running it with the same file changes nothing (F2.3.2).
 */
@Component
public class SupplierSeeder implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(SupplierSeeder.class);

    private final SupplierRepository repository;
    private final SupplierService service;
    private final SupplierProperties properties;
    private final SeedCsvReader reader = new SeedCsvReader();

    public SupplierSeeder(SupplierRepository repository, SupplierService service, SupplierProperties properties) {
        this.repository = repository;
        this.service = service;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (properties.seedFile().isEmpty()) {
            log.info("SUPPLIER_SEED_FILE not set; skipping supplier seeding");
            return;
        }
        seed(Path.of(properties.seedFile()));
    }

    public SeedReport seed(Path file) {
        SeedFile parsed = reader.read(file);
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        for (SeedRowError error : parsed.errors()) {
            log.warn("Skipped seed row {}: {}", error.rowNumber(), error.reason());
            skipped++;
        }

        Set<String> keysInFile = new HashSet<>();
        Set<String> touchedIds = new HashSet<>();
        for (SeedRow row : parsed.rows()) {
            if (!keysInFile.add(row.details().naturalKey())) {
                log.warn("Skipped seed row {}: duplicate of an earlier row (same name and building)", row.rowNumber());
                skipped++;
                continue;
            }
            Optional<Supplier> existing = row.id() != null
                    ? repository.findById(row.id())
                    : repository.findByNaturalKey(row.details().naturalKey());
            if (row.id() != null && existing.isEmpty()) {
                log.warn("Skipped seed row {}: unknown supplier ID {}", row.rowNumber(), row.id());
                skipped++;
                continue;
            }
            try {
                if (existing.isPresent()) {
                    Supplier current = existing.get();
                    touchedIds.add(current.id());
                    if (current.details().equals(row.details())) {
                        unchanged++;
                    } else {
                        repository.update(current.id(), row.details(), current.active());
                        updated++;
                    }
                } else {
                    touchedIds.add(repository.create(row.details(), SupplierSource.SEED).id());
                    created++;
                }
            } catch (DuplicateSupplierException e) {
                log.warn("Skipped seed row {}: {}", row.rowNumber(), e.getMessage());
                skipped++;
            }
        }

        // F2.4: seed records no longer in the file are deactivated, never deleted. Records created
        // by administrators are left alone so a restart cannot undo their changes.
        int deactivated = 0;
        for (Supplier supplier : repository.findAll()) {
            if (supplier.source() == SupplierSource.SEED && supplier.active() && !touchedIds.contains(supplier.id())) {
                repository.update(supplier.id(), supplier.details(), false);
                deactivated++;
            }
        }
        service.invalidateCache();

        SeedReport report = new SeedReport(created, updated, unchanged, deactivated, skipped);
        log.info("Supplier seed {}: {} created, {} updated, {} unchanged, {} deactivated, {} skipped",
                file.getFileName(), report.created(), report.updated(), report.unchanged(), report.deactivated(),
                report.skipped());
        return report;
    }
}
