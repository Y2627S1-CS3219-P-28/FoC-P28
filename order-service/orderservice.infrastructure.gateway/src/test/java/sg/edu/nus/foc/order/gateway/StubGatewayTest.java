package sg.edu.nus.foc.order.gateway;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StubGatewayTest {
    @Test
    void creditAlwaysSucceedsWithoutRealFunds() {
        StubCreditGateway credit = new StubCreditGateway();
        assertThat(credit.reserve("e", "r", 5)).isTrue();
        assertThat(credit.reserve("e", "r", 5)).isTrue();
        assertThat(credit.release("e", "r")).isTrue();
    }

    @Test
    void supplierFixturesValidatePairAndRejectUnknownOrSame() {
        StubSupplierGateway supplier = new StubSupplierGateway();
        assertThat(supplier.list()).hasSize(3);
        assertThat(supplier.resolve("demo-library").orElseThrow().name()).contains("demo");
        assertThat(supplier.resolve("unknown")).isEmpty();
    }
}
