package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderCheckpoint;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;
import sg.edu.nus.foc.order.infrastructure.CommandReceiptRepository;
import sg.edu.nus.foc.order.domain.CommandReceipt;

@Service
public class OrderTransitionService {
    private final OrderRepository orders; private final CheckpointRepository checkpoints; private final CommandReceiptRepository receipts; private final UserServicePort users; private final CreditServicePort credits; private final OrderAuditLogger audit;
    public OrderTransitionService(OrderRepository orders, CheckpointRepository checkpoints, CommandReceiptRepository receipts, UserServicePort users, CreditServicePort credits, OrderAuditLogger audit) { this.orders=orders; this.checkpoints=checkpoints; this.receipts=receipts; this.users=users; this.credits=credits; this.audit=audit; }
    @Transactional public Order start(String commandId,String id,String actor,long v,String auth){return courier("START",commandId,id,actor,v,auth,Order::start);}
    @Transactional public Order pickup(String commandId,String id,String actor,long v,String auth){return courier("PICKUP",commandId,id,actor,v,auth,Order::markPickedUp);}
    @Transactional public Order deliver(String commandId,String id,String actor,long v,String auth){return courier("DELIVER",commandId,id,actor,v,auth,Order::markDelivered);}
    @Transactional public Order complete(String commandId,String id,String actor,long v,String auth){
        var previous=receipts.findByOperationAndCommandId("COMPLETE",commandId); if(previous.isPresent()) return orders.findById(previous.get().getOrderId()).orElseThrow();
        String authenticatedActor=users.verifyRequester(actor,auth); Order o=lock(id); o.confirmCompletion(authenticatedActor,v);
        credits.settle(commandId,o.getId(),o.getRequesterId(),o.getCourierId(),o.getOfferedCredits(),v,auth); audit.dependency("credit-service", "settle", o.getId(), "accepted");
        Order saved=orders.save(o); receipts.save(new CommandReceipt("COMPLETE",commandId,saved.getId(),Instant.now())); audit.action("COMPLETE", saved.getId(), authenticatedActor, commandId, "accepted"); return saved;
    }
    @Transactional public Order cancel(String commandId,String id,String actor,long v,String auth){
        var previous=receipts.findByOperationAndCommandId("CANCEL",commandId); if(previous.isPresent()) return orders.findById(previous.get().getOrderId()).orElseThrow();
        String authenticatedActor=users.verifyRequester(actor,auth); Order o=lock(id); o.cancelOpen(authenticatedActor,v);
        credits.release(commandId,o.getId(),o.getRequesterId(),o.getOfferedCredits(),"CANCELLED",v,auth); audit.dependency("credit-service", "release", o.getId(), "accepted");
        Order saved=orders.save(o); receipts.save(new CommandReceipt("CANCEL",commandId,saved.getId(),Instant.now())); audit.action("CANCEL", saved.getId(), authenticatedActor, commandId, "accepted"); return saved;
    }
    private Order courier(String operation,String commandId,String id,String actor,long v,String auth,BiConsumer3<Order,String,Long> action){String authenticatedActor=users.verifyCourier(actor,auth); return apply(operation,commandId,id,authenticatedActor,v,action,true);}
    private Order requester(String operation,String commandId,String id,String actor,long v,String auth,BiConsumer3<Order,String,Long> action,boolean checkpoint){String authenticatedActor=users.verifyRequester(actor,auth); return apply(operation,commandId,id,authenticatedActor,v,action,checkpoint);}
    private Order apply(String operation,String commandId,String id,String actor,long v,BiConsumer3<Order,String,Long> action,boolean checkpoint){var previous=receipts.findByOperationAndCommandId(operation,commandId);if(previous.isPresent())return orders.findById(previous.get().getOrderId()).orElseThrow();Order o=lock(id);action.accept(o,actor,v);if(checkpoint)checkpoints.save(new OrderCheckpoint(o.getId(),o.getStatus(),Instant.now(),actor,null));Order saved=orders.save(o);receipts.save(new CommandReceipt(operation,commandId,saved.getId(),Instant.now()));return saved;}
    private Order lock(String id){return orders.lockById(id).orElseThrow(()->sg.edu.nus.foc.order.domain.OrderProblem.notFound("Order not found."));}
    @FunctionalInterface private interface BiConsumer3<T,U,V>{void accept(T t,U u,V v);}
}
