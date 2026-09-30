package sg.edu.nus.foc.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.foc.order.application.*;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.domain.RepostPlan;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    private final OrderCreationService creation;
    private final OrderAssignmentService assignment;
    private final OrderTransitionService transitions;
    private final OrderQueryService queries;
    private final OrderRepostService reposts;
    private final LifecycleProcessingService lifecycle;
    private final UserServicePort users;
    private final String lifecycleToken;

    public OrderController(OrderCreationService c, OrderAssignmentService a, OrderTransitionService t,
                           OrderQueryService q, OrderRepostService r, LifecycleProcessingService l,
                           UserServicePort u, @Value("${order.lifecycle-token}") String lifecycleToken) {
        creation = c; assignment = a; transitions = t; queries = q; reposts = r; lifecycle = l; users = u; this.lifecycleToken = lifecycleToken;
    }

    @Operation(summary = "Create an order and reserve credits")
    @PostMapping
    public OrderDtos.View create(@Valid @RequestBody OrderDtos.Create r,
                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        RepostPlan plan = r.automaticRepost() ? new RepostPlan(true, r.repostDueAt(), r.repostCreditAmount(), r.repostDeliveryDurationMinutes()) : null;
        return OrderDtos.View.of(creation.create(r.commandId(), r.requesterId(), r.itemDescription(), r.pickupSupplierId(), r.deliverySupplierId(), r.offeredCredits(), r.deliveryTimeLimitMinutes(), r.expiresAt(), plan, auth));
    }

    @Operation(summary = "Get an order by ID")
    @GetMapping("/{id}")
    public OrderDtos.View get(@PathVariable String id) { return OrderDtos.View.of(queries.get(id)); }

    @Operation(summary = "List available open orders")
    @GetMapping("/available")
    public OrderDtos.PageView<OrderDtos.View> available(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return OrderDtos.PageView.of(queries.available(page - 1, size).map(OrderDtos.View::of));
    }

    @Operation(summary = "List orders for the authenticated requester or courier")
    @GetMapping("/mine")
    public OrderDtos.PageView<OrderDtos.View> mine(@RequestParam String mode, @RequestParam String userId,
                                                    @RequestHeader(value = "Authorization", required = false) String auth,
                                                    @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return switch (mode.toLowerCase()) {
            case "requester" -> { String authenticatedUser = users.verifyRequester(userId, auth); yield OrderDtos.PageView.of(queries.requestedBy(authenticatedUser, page - 1, size).map(OrderDtos.View::of)); }
            case "courier" -> { String authenticatedUser = users.verifyCourier(userId, auth); yield OrderDtos.PageView.of(queries.courierFor(authenticatedUser, page - 1, size).map(OrderDtos.View::of)); }
            default -> throw new OrderProblem("VALIDATION_ERROR", "Mode must be requester or courier.");
        };
    }

    @Operation(summary = "Accept an available order as a courier")
    @PostMapping("/{id}/accept")
    public OrderDtos.View accept(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(assignment.accept(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Start an accepted order")
    @PostMapping("/{id}/start")
    public OrderDtos.View start(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(transitions.start(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Mark an order as picked up")
    @PostMapping("/{id}/pickup")
    public OrderDtos.View pickup(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(transitions.pickup(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Mark an order as delivered")
    @PostMapping("/{id}/deliver")
    public OrderDtos.View deliver(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(transitions.deliver(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Confirm completion and settle credits")
    @PostMapping("/{id}/complete")
    public OrderDtos.View complete(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(transitions.complete(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Cancel an open order and release credits")
    @PostMapping("/{id}/cancel")
    public OrderDtos.View cancel(@PathVariable String id, @Valid @RequestBody OrderDtos.Actor r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(transitions.cancel(r.commandId(), id, r.actorId(), r.expectedVersion(), auth)); }

    @Operation(summary = "Reject legacy post-creation repost configuration")
    @PostMapping("/{id}/repost/configure")
    public OrderDtos.View configure(@PathVariable String id, @Valid @RequestBody OrderDtos.RepostConfig r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(reposts.configure(r.commandId(), id, r.actorId(), r.expectedVersion(), new RepostPlan(r.enabled(), r.dueAt(), r.creditAmount(), r.deliveryDurationMinutes()), auth)); }

    @Operation(summary = "Get a manual repost draft for an expired order")
    @GetMapping("/{id}/repost-draft")
    public OrderDtos.Draft draft(@PathVariable String id, @RequestParam String actorId,
                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        String authenticatedActor = users.verifyRequester(actorId, auth);
        Order o = queries.get(id);
        if (!o.getRequesterId().equals(authenticatedActor)) throw new OrderProblem("FORBIDDEN", "Only the requester may view the draft.");
        if (o.getStatus() != OrderStatus.EXPIRED || o.getRepostedOrderId() != null) throw new OrderProblem("CONFLICT", "Order is not eligible for a repost draft.");
        return new OrderDtos.Draft(o.getId(), o.getItemDescription(), o.getPickupSupplierId(), o.getDeliverySupplierId(), o.getOfferedCredits(), o.getDeliveryTimeLimitMinutes(), o.getExpiresAt());
    }

    @Operation(summary = "Create a requester-reviewed repost")
    @PostMapping("/{id}/repost")
    public OrderDtos.View repost(@PathVariable String id, @Valid @RequestBody OrderDtos.ManualRepost r, @RequestHeader(value = "Authorization", required = false) String auth) { return OrderDtos.View.of(reposts.manual(r.commandId(), id, r.actorId(), r.expectedVersion(), r.itemDescription(), r.offeredCredits(), r.deliveryTimeLimitMinutes(), r.expiresAt(), auth)); }

    @Operation(summary = "Expire due unaccepted orders using the lifecycle token")
    @PostMapping("/internal/lifecycle/expire")
    public Map<String, Integer> expire(@RequestHeader("X-Lifecycle-Token") String token) { checkToken(token); return Map.of("expired", lifecycle.expireDue(Instant.now(), "Bearer " + token)); }

    @Operation(summary = "Create due automatic reposts using the lifecycle token")
    @PostMapping("/internal/lifecycle/repost")
    public Map<String, Integer> automaticRepost(@RequestHeader("X-Lifecycle-Token") String token) { checkToken(token); return Map.of("reposted", lifecycle.repostDue(Instant.now(), "Bearer " + token)); }

    private void checkToken(String token) { if (token == null || !token.equals(lifecycleToken)) throw new OrderProblem("FORBIDDEN", "Invalid lifecycle token."); }
}
