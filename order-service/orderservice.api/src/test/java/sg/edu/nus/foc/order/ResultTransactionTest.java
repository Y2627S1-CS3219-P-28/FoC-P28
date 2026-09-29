package sg.edu.nus.foc.order;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import sg.edu.nus.foc.order.application.BaseService;
import sg.edu.nus.foc.order.application.ResultWrapper;
import sg.edu.nus.foc.order.domain.*;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest(properties = "spring.profiles.active=test")
@Import({PostgresTestConfiguration.class, ResultTransactionTest.Config.class})
class ResultTransactionTest {
    @Autowired Probe probe;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean ErrandRepository errands;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean CreditGateway credits;
    @Autowired sg.edu.nus.foc.order.application.ErrandWorkflow workflow;

    @Test
    void earlyFailureCompletesWithoutRollbackOrWrites() {
        ResultWrapper<String> result = probe.execute("missing", Mode.EARLY);
        assertThat(result.getError().getCode()).isEqualTo(OrderProblem.Code.NOT_FOUND);
        assertThat(probe.completion()).isEqualTo(TransactionSynchronization.STATUS_COMMITTED);
    }

    @Test
    void explicitFailureRollsBackManagedEntityWithoutSaveCall() {
        Errand errand = fixture();
        assertThat(probe.execute(errand.getId(), Mode.FAILURE).isError()).isTrue();
        assertThat(probe.completion()).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(errands.findById(errand.getId()).orElseThrow().getStatus())
                .isEqualTo(Errand.Status.OPEN);
    }

    @Test
    void unexpectedExceptionRollsBackAndPreservesCause() {
        Errand errand = fixture();
        assertThatThrownBy(() -> probe.execute(errand.getId(), Mode.UNEXPECTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Technical failure");
        assertThat(probe.completion()).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(errands.findById(errand.getId()).orElseThrow().getStatus())
                .isEqualTo(Errand.Status.OPEN);
    }

    @Test
    void successfulManagedChangeCommits() {
        Errand errand = fixture();
        assertThat(probe.execute(errand.getId(), Mode.SUCCESS).getData()).isEqualTo("saved");
        assertThat(probe.completion()).isEqualTo(TransactionSynchronization.STATUS_COMMITTED);
        assertThat(errands.findById(errand.getId()).orElseThrow().getStatus())
                .isEqualTo(Errand.Status.ACCEPTED);
    }

    @Test
    void writeFailureRollsBackCreationAndReleasesReservedCredit() {
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            entityManager.persist(invocation.getArgument(0));
                            entityManager.flush();
                            throw new IllegalStateException("Failure after insert");
                        })
                .when(errands)
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Errand.class));
        sg.edu.nus.foc.order.application.ErrandWorkflow.CreateInput input =
                new sg.edu.nus.foc.order.application.ErrandWorkflow.CreateInput(
                        UUID.randomUUID().toString(),
                        "r",
                        "Bring my lunch",
                        "demo-canteen",
                        "demo-library",
                        5,
                        30,
                        Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> workflow.create(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failure after insert");
        org.mockito.ArgumentCaptor<String> id = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(credits)
                .reserve(
                        id.capture(),
                        org.mockito.ArgumentMatchers.eq("r"),
                        org.mockito.ArgumentMatchers.eq(5L));
        org.mockito.Mockito.verify(credits).release(id.getValue(), "r");
        assertThat(errands.findById(id.getValue())).isEmpty();
    }

    private Errand fixture() {
        Instant now = Instant.now();
        return errands.saveAndFlush(
                new Errand(
                        UUID.randomUUID().toString(),
                        "r",
                        "Bring my lunch",
                        "demo-canteen",
                        "demo-library",
                        5,
                        30,
                        now.plusSeconds(3600),
                        now,
                        "hash"));
    }

    enum Mode {
        EARLY,
        FAILURE,
        UNEXPECTED,
        SUCCESS
    }

    @TestConfiguration
    static class Config {
        @Bean
        Probe probe(ErrandRepository errands, OrderRepository orders) {
            return new Probe(errands, orders);
        }
    }

    static class Probe extends BaseService {
        private final ErrandRepository errands;
        private final OrderRepository orders;
        int completion;

        Probe(ErrandRepository errands, OrderRepository orders) {
            this.errands = errands;
            this.orders = orders;
        }

        public int completion() {
            return completion;
        }

        @Transactional
        public ResultWrapper<String> execute(String id, Mode mode) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            completion = status;
                        }
                    });
            if (mode == Mode.EARLY) {
                return error(new OrderProblem(OrderProblem.Code.NOT_FOUND, "Not found"));
            }
            Errand errand = errands.findById(id).orElseThrow();
            String orderId = UUID.randomUUID().toString();
            assertThat(errand.accept("c", orderId, errand.getVersion(), Instant.now())).isEmpty();
            if (mode == Mode.FAILURE) {
                return rollbackError(OrderProblem.conflict("Failure after mutation"));
            }
            if (mode == Mode.UNEXPECTED) {
                throw new IllegalStateException("Technical failure");
            }
            orders.save(new Order(orderId, errand, "c", Instant.now()));
            return ok("saved");
        }
    }
}
