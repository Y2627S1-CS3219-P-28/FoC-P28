package sg.edu.nus.foc.order.api;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.foc.order.application.*;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.domain.OrderStatus;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderCreationService creation; private final OrderAssignmentService assignment; private final OrderTransitionService transitions; private final OrderQueryService queries; private final OrderRepostService reposts; private final LifecycleProcessingService lifecycle; private final String lifecycleToken;
    public OrderController(OrderCreationService c,OrderAssignmentService a,OrderTransitionService t,OrderQueryService q,OrderRepostService r,LifecycleProcessingService l,@Value("${order.lifecycle-token}") String lifecycleToken){creation=c;assignment=a;transitions=t;queries=q;reposts=r;lifecycle=l;this.lifecycleToken=lifecycleToken;}
    @PostMapping public OrderDtos.View create(@Valid @RequestBody OrderDtos.Create r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(creation.create(r.commandId(),r.requesterId(),r.itemDescription(),r.pickupSupplierId(),r.deliverySupplierId(),r.offeredCredits(),r.deliveryTimeLimitMinutes(),r.expiresAt(),auth));}
    @GetMapping("/{id}") public OrderDtos.View get(@PathVariable String id){return OrderDtos.View.of(queries.get(id));}
    @GetMapping("/available") public Page<OrderDtos.View> available(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return queries.available(page,size).map(OrderDtos.View::of);}
    @PostMapping("/{id}/accept") public OrderDtos.View accept(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(assignment.accept(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/start") public OrderDtos.View start(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(transitions.start(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/pickup") public OrderDtos.View pickup(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(transitions.pickup(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/deliver") public OrderDtos.View deliver(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(transitions.deliver(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/complete") public OrderDtos.View complete(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(transitions.complete(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/cancel") public OrderDtos.View cancel(@PathVariable String id,@Valid @RequestBody OrderDtos.Actor r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(transitions.cancel(r.commandId(),id,r.actorId(),r.expectedVersion(),auth));}
    @PostMapping("/{id}/repost/configure") public OrderDtos.View configure(@PathVariable String id,@Valid @RequestBody OrderDtos.RepostConfig r){return OrderDtos.View.of(reposts.configure(r.commandId(),id,r.actorId(),r.expectedVersion(),new RepostPlan(r.enabled(),r.dueAt(),r.creditAmount(),r.deliveryDurationMinutes())));}
    @GetMapping("/{id}/repost-draft") public OrderDtos.Draft draft(@PathVariable String id,@RequestParam String actorId){Order o=queries.get(id);if(!o.getRequesterId().equals(actorId))throw new OrderProblem("FORBIDDEN","Only the requester may view the draft.");if(o.getStatus()!=OrderStatus.EXPIRED||o.getRepostedOrderId()!=null)throw new OrderProblem("CONFLICT","Order is not eligible for a repost draft.");return new OrderDtos.Draft(o.getId(),o.getItemDescription(),o.getPickupSupplierId(),o.getDeliverySupplierId(),o.getOfferedCredits(),o.getDeliveryTimeLimitMinutes(),o.getExpiresAt());}
    @PostMapping("/{id}/repost") public OrderDtos.View repost(@PathVariable String id,@Valid @RequestBody OrderDtos.ManualRepost r,@RequestHeader(value="Authorization",required=false) String auth){return OrderDtos.View.of(reposts.manual(r.commandId(),id,r.actorId(),r.expectedVersion(),r.itemDescription(),r.offeredCredits(),r.deliveryTimeLimitMinutes(),r.expiresAt(),auth));}
    @PostMapping("/internal/lifecycle/expire") public Map<String,Integer> expire(@RequestHeader("X-Lifecycle-Token") String token){checkToken(token);return Map.of("expired",lifecycle.expireDue(Instant.now()));}
    @PostMapping("/internal/lifecycle/repost") public Map<String,Integer> automaticRepost(@RequestHeader("X-Lifecycle-Token") String token){checkToken(token);return Map.of("reposted",lifecycle.repostDue(Instant.now()));}
    private void checkToken(String token){if(token==null||!token.equals(lifecycleToken))throw new OrderProblem("FORBIDDEN","Invalid lifecycle token.");}
}
