package sg.edu.nus.foc.supplier.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sg.edu.nus.foc.supplier.support.Suppliers.details;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sg.edu.nus.foc.supplier.config.SupplierProperties;
import sg.edu.nus.foc.supplier.supplier.Supplier;
import sg.edu.nus.foc.supplier.supplier.SupplierService;
import sg.edu.nus.foc.supplier.supplier.SupplierSource;
import sg.edu.nus.foc.supplier.support.InMemorySupplierRepository;

class SupplierSeederTest {

    private static final String HEADER =
            "Name,Type,Building,Floor,Location Description,Latitude,Longitude,StartingTime,ClosingTime,ImageURL\n";
    private static final String COOL_SPOT = "Cool Spot,Food,Com2,1,Opp LT16,1.294,103.7738,0900hrs,2130hrs,\n";
    private static final String PRINTER = "Printer @ Com 2,Printing,Com 2,1,Next to LT19,1.2938,103.7744,0000hrs,2359hrs,\n";

    @TempDir
    Path dir;

    private InMemorySupplierRepository repository;
    private SupplierSeeder seeder;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-25T04:00:00Z"), ZoneOffset.UTC);
        repository = new InMemorySupplierRepository(clock);
        SupplierProperties properties = new SupplierProperties(null, null, null, null, Duration.ofSeconds(60), null);
        seeder = new SupplierSeeder(repository, new SupplierService(repository, clock, properties), properties);
    }

    private Path csv(String rows) throws IOException {
        Path file = dir.resolve("seed-" + System.nanoTime() + ".csv");
        Files.writeString(file, HEADER + rows);
        return file;
    }

    @Test
    void firstLoadCreatesActiveSeedRecords() throws IOException {
        SeedReport report = seeder.seed(csv(COOL_SPOT + PRINTER));
        assertThat(report).isEqualTo(new SeedReport(2, 0, 0, 0, 0));
        assertThat(repository.findAll()).allSatisfy(s -> {
            assertThat(s.active()).isTrue();
            assertThat(s.source()).isEqualTo(SupplierSource.SEED);
        });
    }

    @Test
    void reloadingTheSameFileChangesNothing() throws IOException {
        Path file = csv(COOL_SPOT + PRINTER);
        seeder.seed(file);
        assertThat(seeder.seed(file)).isEqualTo(new SeedReport(0, 0, 2, 0, 0));
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void changedRowUpdatesTheExistingRecordKeepingItsId() throws IOException {
        seeder.seed(csv(COOL_SPOT));
        String id = repository.findAll().getFirst().id();
        SeedReport report = seeder.seed(csv(COOL_SPOT.replace("2130hrs", "2200hrs")));
        assertThat(report.updated()).isEqualTo(1);
        assertThat(repository.findById(id).orElseThrow().details().closingTime()).hasToString("22:00");
    }

    @Test
    void seedRecordsMissingFromTheFileAreDeactivatedButAdminRecordsAreKept() throws IOException {
        seeder.seed(csv(COOL_SPOT + PRINTER));
        Supplier admin = repository.create(details("Admin Kiosk", "Food", "UTown"), SupplierSource.ADMIN);

        SeedReport report = seeder.seed(csv(COOL_SPOT));
        assertThat(report.deactivated()).isEqualTo(1);
        assertThat(repository.findAll()).hasSize(3);
        assertThat(repository.findAll()).filteredOn(s -> s.details().name().startsWith("Printer"))
                .singleElement().satisfies(s -> assertThat(s.active()).isFalse());
        assertThat(repository.findById(admin.id()).orElseThrow().active()).isTrue();
    }

    @Test
    void reloadDoesNotReactivateAnAdminDeactivation() throws IOException {
        Path file = csv(COOL_SPOT);
        seeder.seed(file);
        Supplier s = repository.findAll().getFirst();
        repository.update(s.id(), s.details(), false);
        seeder.seed(file);
        assertThat(repository.findById(s.id()).orElseThrow().active()).isFalse();
    }

    @Test
    void invalidAndDuplicateRowsAreSkippedWithoutBlockingOthers() throws IOException {
        SeedReport report = seeder.seed(csv(COOL_SPOT + ",Food,B,1,,1,1,0900hrs,1800hrs,\n" + COOL_SPOT + PRINTER));
        assertThat(report).isEqualTo(new SeedReport(2, 0, 0, 0, 2));
    }

    @Test
    void idColumnUpdatesThatSupplierAndUnknownIdsAreSkipped() throws IOException {
        seeder.seed(csv(COOL_SPOT));
        String id = repository.findAll().getFirst().id();
        String idHeader = "Id," + HEADER;
        Path file = dir.resolve("with-ids.csv");
        Files.writeString(file, idHeader
                + id + ",Cool Spot Renamed,Food,Com2,1,Opp LT16,1.294,103.7738,0900hrs,2130hrs,\n"
                + "unknown,Ghost,Food,Com2,1,,1,1,0900hrs,2130hrs,\n");
        SeedReport report = seeder.seed(file);
        assertThat(report.updated()).isEqualTo(1);
        assertThat(report.skipped()).isEqualTo(1);
        assertThat(repository.findById(id).orElseThrow().details().name()).isEqualTo("Cool Spot Renamed");
    }

    @Test
    void renameOntoAnotherSupplierIsSkipped() throws IOException {
        seeder.seed(csv(COOL_SPOT + PRINTER));
        String printerId = repository.findAll().stream()
                .filter(s -> s.details().name().startsWith("Printer")).findFirst().orElseThrow().id();
        Path file = dir.resolve("clash.csv");
        Files.writeString(file, "Id," + HEADER + printerId + "," + COOL_SPOT);
        assertThat(seeder.seed(file).skipped()).isEqualTo(1);
    }

    @Test
    void startupWithoutSeedFileDoesNothingAndBrokenFileFailsStartup() {
        seeder.afterSingletonsInstantiated();
        assertThat(repository.findAll()).isEmpty();

        SupplierProperties broken = new SupplierProperties(null, null, null, dir.resolve("missing.csv").toString(),
                null, null);
        Clock clock = Clock.systemUTC();
        SupplierSeeder failing = new SupplierSeeder(repository, new SupplierService(repository, clock, broken), broken);
        assertThatThrownBy(failing::afterSingletonsInstantiated).isInstanceOf(SeedFileException.class);
    }
}
