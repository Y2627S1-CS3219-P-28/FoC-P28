package sg.edu.nus.foc.order.gateway;
import org.junit.jupiter.api.Test;
import sg.edu.nus.foc.order.domain.OrderProblem;
import static org.assertj.core.api.Assertions.*;
class StubGatewayTest {
    @Test void creditAlwaysSucceedsWithoutRealFunds() {
        var credit=new StubCreditGateway();
        assertThat(credit.reserve("e","r",5)).isTrue();
        assertThat(credit.reserve("e","r",5)).isTrue();
        assertThat(credit.release("e","r")).isTrue();
    }
    @Test void supplierFixturesValidatePairAndRejectUnknownOrSame() {
        var supplier=new StubSupplierGateway();
        assertThat(supplier.list()).hasSize(3);
        assertThat(supplier.resolve("demo-library").name()).contains("demo");
        supplier.validatePair("demo-canteen","demo-library");
        assertThatThrownBy(()->supplier.resolve("unknown")).isInstanceOf(OrderProblem.class);
        assertThatThrownBy(()->supplier.validatePair("demo-canteen","demo-canteen")).isInstanceOf(OrderProblem.class);
    }
}
