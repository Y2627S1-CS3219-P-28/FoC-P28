package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCompletionHold;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.repository.OrderCompletionHoldRepository;
import sg.edu.nus.foc.order.domain.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderCompletionHoldService {

    private final OrderRepository orders;
    private final OrderCompletionHoldRepository holds;
    private final UserServicePort users;
    private final OrderAuditLogger audit;

    @Transactional
    public Order place(String orderId, String reportId, String requesterId, String authorization) {
        if (reportId == null || reportId.isBlank() || reportId.length() > 128
                || !reportId.equals(reportId.strip())) {
            throw new OrderProblem("VALIDATION_ERROR", "A report ID of 1 through 128 characters is required.");
        }
        String actor = users.verifyRequester(requesterId, authorization);
        Order order = orders.getForUpdate(orderId)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));

        Optional<OrderCompletionHold> previous = holds.findByReportId(reportId);
        if (previous.isPresent()) {
            previous.get().requireBinding(orderId, actor);
            return order;
        }

        order.placeCompletionHold(actor);
        holds.save(new OrderCompletionHold(reportId, orderId, actor, Instant.now()));
        Order saved = orders.save(order);
        audit.action("PLACE_COMPLETION_HOLD", orderId, actor, reportId, "accepted");
        return saved;
    }

    @Transactional
    public Order release(String orderId, String reportId, String adminId) {
        Order order = orders.getForLifecycleUpdate(orderId)
                .orElseThrow(() -> OrderProblem.notFound("Order not found."));
        OrderCompletionHold hold = holds.findByReportId(reportId)
                .orElseThrow(() -> OrderProblem.notFound("Report hold not found."));
        if (!orderId.equals(hold.getOrderId())) {
            throw OrderProblem.conflict("Report hold belongs to a different order.");
        }
        if (hold.getReleasedAt() != null) return order;
        hold.release(adminId, Instant.now());
        holds.release(hold);
        order.reconcileReportHolds(holds.hasActiveForOrder(orderId));
        Order saved = orders.save(order);
        audit.action("RELEASE_COMPLETION_HOLD", orderId, adminId, reportId, "accepted");
        return saved;
    }
}
