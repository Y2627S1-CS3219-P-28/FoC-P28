package sg.edu.nus.foc.supplier.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sg.edu.nus.foc.supplier.support.Suppliers.details;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.supplier.support.FirestoreEmulator;

class FirestoreSupplierRepositoryIntegrationTest {

    private static final String DATABASE = "repository-test";
    private static Firestore firestore;
    private FirestoreSupplierRepository repository;

    @BeforeAll
    static void connect() {
        firestore = FirestoreEmulator.client(DATABASE);
    }

    @AfterAll
    static void disconnect() throws Exception {
        firestore.close();
    }

    @BeforeEach
    void reset() {
        FirestoreEmulator.clear(DATABASE);
        repository = new FirestoreSupplierRepository(firestore,
                Clock.fixed(Instant.parse("2026-09-25T04:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsAndReadsBackEveryField() {
        SupplierDetails d = new SupplierDetails("Anna's x Soup Union", "Food", "Central Library", "1",
                "Next to NUS Co-op", 1.296444, 103.773032, java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(18, 0), "https://example.com/anna.jpeg");
        Supplier created = repository.create(d, SupplierSource.SEED);

        Supplier read = repository.findById(created.id()).orElseThrow();
        assertThat(read).isEqualTo(created);
        assertThat(read.details()).isEqualTo(d);
        assertThat(read.active()).isTrue();
        assertThat(read.createdAt()).isEqualTo(Instant.parse("2026-09-25T04:00:00Z"));
        assertThat(repository.findAll()).containsExactly(created);
        assertThat(repository.findByNaturalKey(d.naturalKey())).contains(created);
    }

    @Test
    void rejectsDuplicateNameAndBuilding() {
        Supplier first = repository.create(details("Cool Spot", "Food", "Com2"), SupplierSource.ADMIN);
        assertThatThrownBy(() -> repository.create(details("COOL SPOT", "Food", "com2"), SupplierSource.ADMIN))
                .isInstanceOf(DuplicateSupplierException.class)
                .satisfies(e -> assertThat(((DuplicateSupplierException) e).existingSupplierId()).isEqualTo(first.id()));
    }

    @Test
    void updateKeepsIdAndSourceAndMovesTheUniquenessKey() {
        Supplier created = repository.create(details("Cool Spot", "Food", "Com2"), SupplierSource.SEED);
        Supplier renamed = repository.update(created.id(), details("Cool Spot 2", "Food", "Com2"), false);

        assertThat(renamed.id()).isEqualTo(created.id());
        assertThat(renamed.source()).isEqualTo(SupplierSource.SEED);
        assertThat(renamed.active()).isFalse();
        assertThat(repository.findByNaturalKey(NaturalKey.of("Cool Spot", "Com2"))).isEmpty();
        assertThat(repository.findByNaturalKey(NaturalKey.of("Cool Spot 2", "Com2"))).contains(renamed);
        // The old name is free again.
        assertThat(repository.create(details("Cool Spot", "Food", "Com2"), SupplierSource.ADMIN).id())
                .isNotEqualTo(created.id());
    }

    @Test
    void updateWithoutRenameKeepsKey() {
        Supplier created = repository.create(details("Cool Spot", "Food", "Com2"), SupplierSource.SEED);
        repository.update(created.id(), details("Cool Spot", "Food/Coffee", "Com2"), true);
        assertThat(repository.findByNaturalKey(created.details().naturalKey()).orElseThrow().details().type())
                .isEqualTo("Food/Coffee");
    }

    @Test
    void updateRejectsRenameOntoAnotherSupplier() {
        repository.create(details("A", "Food", "Com2"), SupplierSource.ADMIN);
        Supplier b = repository.create(details("B", "Food", "Com2"), SupplierSource.ADMIN);
        assertThatThrownBy(() -> repository.update(b.id(), details("a", "Food", "Com2"), true))
                .isInstanceOf(DuplicateSupplierException.class);
    }

    @Test
    void updateOfUnknownOrInvalidIdIsNotFound() {
        assertThatThrownBy(() -> repository.update("missing", details("A", "Food", "Com2"), true))
                .isInstanceOf(SupplierNotFoundException.class);
        assertThatThrownBy(() -> repository.update("a/b", details("A", "Food", "Com2"), true))
                .isInstanceOf(SupplierNotFoundException.class);
    }

    @Test
    void findAllByIdSkipsUnknownAndInvalidIds() {
        Supplier a = repository.create(details("A", "Food", "Com2"), SupplierSource.ADMIN);
        assertThat(repository.findAllById(List.of(a.id(), "missing", "a/b", "..", "__x__", " ")))
                .containsExactly(a);
        assertThat(repository.findAllById(List.of("a/b"))).isEmpty();
        assertThat(repository.findById("..")).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }
}
