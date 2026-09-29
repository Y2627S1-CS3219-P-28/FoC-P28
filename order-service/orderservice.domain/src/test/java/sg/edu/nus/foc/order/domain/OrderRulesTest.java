package sg.edu.nus.foc.order.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class OrderRulesTest {
    private final Instant now = Instant.parse("2026-09-29T12:00:00Z");

    Errand errand() {
        return new Errand(
                "e", "r", "Bring my lunch", "p", "d", 5, 30, now.plusSeconds(3600), now, "hash");
    }

    @Test
    void acceptanceAndCheckpointsPreserveIdentitiesAndTime() {
        Errand e = errand();
        assertThat(e.getId()).isEqualTo("e");
        assertThat(e.getRequesterId()).isEqualTo("r");
        assertThat(e.getDescription()).isEqualTo("Bring my lunch");
        assertThat(e.getPickupSupplierId()).isEqualTo("p");
        assertThat(e.getDeliverySupplierId()).isEqualTo("d");
        assertThat(e.getCreditAmount()).isEqualTo(5);
        assertThat(e.getCreatedAt()).isEqualTo(now);
        assertThat(e.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(e.getRequestFingerprint()).isEqualTo("hash");
        e.accept("c", "o", 0, now);
        assertThat(e.getStatus()).isEqualTo(Errand.Status.ACCEPTED);
        assertThat(e.getOrderId()).isEqualTo("o");
        Order o = new Order("o", e, "c", now);
        assertThat(o.getErrandId()).isEqualTo("e");
        assertThat(o.getDeliveryDurationMinutes()).isEqualTo(30);
        assertThat(o.getAcceptedAt()).isEqualTo(now);
        assertThat(o.getDeliveryDeadline()).isNull();
        o.progress("c", 0, Order.Status.IN_PROGRESS, now.plusSeconds(1));
        o.progress("c", 0, Order.Status.PICKED_UP, now.plusSeconds(2));
        o.progress("c", 0, Order.Status.DELIVERED, now.plusSeconds(3));
        assertThat(o.getStartedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(o.getPickedUpAt()).isEqualTo(now.plusSeconds(2));
        assertThat(o.getDeliveredAt()).isEqualTo(now.plusSeconds(3));
        assertThat(o.getDeliveryDeadline()).isEqualTo(now.plusSeconds(1801));
        Checkpoint checkpoint = new Checkpoint(o, now.plusSeconds(3), "d");
        assertThat(checkpoint.getId()).isNotBlank();
        assertThat(checkpoint.getOrderId()).isEqualTo("o");
        assertThat(checkpoint.getCourierId()).isEqualTo("c");
        assertThat(checkpoint.getStatus()).isEqualTo(Order.Status.DELIVERED);
        assertThat(checkpoint.getSupplierId()).isEqualTo("d");
        assertThat(checkpoint.getOccurredAt()).isEqualTo(now.plusSeconds(3));
        CommandReceipt receipt = new CommandReceipt("id", "same");
        assertThat(receipt.verify("same")).isEmpty();
        assertThat(receipt.verify("different")).isPresent();
    }

    @Test
    void acceptanceRejectsOwnerExpiredAssignedAndStale() {
        Errand e = errand();
        assertThat(e.accept("r", "o", 0, now)).isPresent();
        assertThat(e.accept("c", "o", 1, now)).isPresent();
        assertThat(e.accept("c", "o", 0, e.getExpiresAt())).isPresent();
        e.accept("c", "o", 0, now);
        assertThat(e.accept("d", "o2", 0, now)).isPresent();
    }

    @Test
    void progressRejectsWrongCourierVersionAndSequence() {
        Order o = new Order("o", errand(), "c", now);
        assertThat(o.progress("r", 0, Order.Status.IN_PROGRESS, now)).isPresent();
        assertThat(o.progress("c", 1, Order.Status.IN_PROGRESS, now)).isPresent();
        assertThat(o.progress("c", 0, Order.Status.PICKED_UP, now)).isPresent();
        assertThat(o.progress("c", 0, Order.Status.ACCEPTED, now)).isPresent();
        assertThat(o.progress("c", 0, null, now)).isPresent();
        assertThat(o.getStatus()).isEqualTo(Order.Status.ACCEPTED);
        assertThat(o.getStartedAt()).isNull();
        OrderProblem problem =
                new OrderProblem(
                        OrderProblem.Code.VALIDATION_ERROR,
                        "invalid",
                        java.util.List.of(new OrderProblem.Detail("field", "message")));
        assertThat(problem.getCode()).isEqualTo(OrderProblem.Code.VALIDATION_ERROR);
        assertThat(problem.getDetails())
                .containsExactly(new OrderProblem.Detail("field", "message"));
    }
}
