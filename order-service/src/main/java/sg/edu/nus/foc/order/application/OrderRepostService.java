package sg.edu.nus.foc.order.application;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.domain.OrderProblem;
import sg.edu.nus.foc.order.domain.RepostPlan;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;
import sg.edu.nus.foc.order.infrastructure.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.CommandReceipt;

@Service
public class OrderRepostService {
    private final OrderRepository orders; private final CheckpointRepository checkpoints; private final CommandReceiptRepository receipts; private final SupplierServicePort suppliers; private final CreditServicePort credits; private final UserServicePort users; private final OrderAuditLogger audit;
    public OrderRepostService(OrderRepository orders,CheckpointRepository checkpoints,CommandReceiptRepository receipts,SupplierServicePort suppliers,CreditServicePort credits,UserServicePort users,OrderAuditLogger audit){this.orders=orders;this.checkpoints=checkpoints;this.receipts=receipts;this.suppliers=suppliers;this.credits=credits;this.users=users;this.audit=audit;}
    @Transactional public Order configure(String commandId,String id,String actor,long version,RepostPlan plan,String authorization){throw OrderProblem.conflict("Automatic repost settings must be chosen when the order is created.");}
    @Transactional public Order automatic(String commandId,String id,Instant now,String authorization){var previous=receipts.findByOperationAndCommandId("AUTO_REPOST",commandId);if(previous.isPresent())return orders.findById(previous.get().getOrderId()).orElseThrow();Order original=lock(id); if(!original.eligibleForAutomaticRepost(now)) throw OrderProblem.conflict("Order is not eligible for automatic repost."); RepostPlan plan=original.getRepostPlan(); suppliers.validatePair(original.getPickupSupplierId(),original.getDeliverySupplierId(),authorization); Order repost=original.createRepost(original.getItemDescription(),plan.creditAmount(),plan.deliveryDurationMinutes(),now,now.plusSeconds(plan.deliveryDurationMinutes()*60L)); credits.reserve(repost.getId(),repost.getRequesterId(),repost.getOfferedCredits(),authorization); original.linkRepost(repost.getId()); orders.save(original); Order saved=orders.save(repost); checkpoints.save(new OrderCheckpoint(saved.getId(),saved.getStatus(),now,saved.getRequesterId(),null)); receipts.save(new CommandReceipt("AUTO_REPOST",commandId,saved.getId(),Instant.now())); audit.dependency("credit-service", "reserve", saved.getId(), "accepted"); audit.action("AUTO_REPOST", saved.getId(), saved.getRequesterId(), commandId, "accepted"); return saved;}
    @Transactional public Order manual(String commandId,String id,String actor,long version,String description,long creditsAmount,int duration,Instant expiresAt,String authorization){var previous=receipts.findByOperationAndCommandId("MANUAL_REPOST",commandId);if(previous.isPresent())return orders.findById(previous.get().getOrderId()).orElseThrow();String authenticatedActor=users.verifyRequester(actor,authorization);Order original=lock(id);original.requireVersion(version);if(!original.getRequesterId().equals(authenticatedActor))throw OrderProblem.forbidden("Only the requester may repost.");if(original.getStatus()!=sg.edu.nus.foc.order.domain.OrderStatus.EXPIRED)throw OrderProblem.conflict("Only an expired order may be reposted.");suppliers.validatePair(original.getPickupSupplierId(),original.getDeliverySupplierId(),authorization);Order repost=original.createRepost(description,creditsAmount,duration,Instant.now(),expiresAt);credits.reserve(repost.getId(),repost.getRequesterId(),repost.getOfferedCredits(),authorization);original.linkRepost(repost.getId());orders.save(original);Order saved=orders.save(repost);checkpoints.save(new OrderCheckpoint(saved.getId(),saved.getStatus(),saved.getCreatedAt(),authenticatedActor,null));receipts.save(new CommandReceipt("MANUAL_REPOST",commandId,saved.getId(),Instant.now()));audit.dependency("credit-service", "reserve", saved.getId(), "accepted"); audit.action("MANUAL_REPOST", saved.getId(), authenticatedActor, commandId, "accepted"); return saved;}
    private Order lock(String id){return orders.lockById(id).orElseThrow(()->OrderProblem.notFound("Order not found."));}
}
