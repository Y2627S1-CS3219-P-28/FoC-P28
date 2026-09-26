package sg.edu.nus.foc.supplier.seed;

/** Outcome of one seed load (F2.5.3). */
public record SeedReport(int created, int updated, int unchanged, int deactivated, int skipped) {
}
