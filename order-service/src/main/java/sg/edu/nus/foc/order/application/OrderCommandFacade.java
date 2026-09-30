package sg.edu.nus.foc.order.application;

import java.time.Instant;
import org.springframework.stereotype.Service;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.RepostPlan;

/** Inbound command boundary retained as the single dispatch point for future command IDs. */
@Service
public class OrderCommandFacade {
    private final OrderCreationService creation;
    private final OrderAssignmentService assignment;
    private final OrderTransitionService transitions;
    private final OrderRepostService reposts;
    public OrderCommandFacade(OrderCreationService creation, OrderAssignmentService assignment, OrderTransitionService transitions, OrderRepostService reposts) {
        this.creation=creation;this.assignment=assignment;this.transitions=transitions;this.reposts=reposts;
    }
    public Order create(String commandId,String requester,String description,String pickup,String delivery,long credits,int duration,Instant expires,RepostPlan repostPlan,String auth){return creation.create(commandId,requester,description,pickup,delivery,credits,duration,expires,repostPlan,auth);}
    public Order accept(String commandId,String id,String actor,long version,String auth){return assignment.accept(commandId,id,actor,version,auth);}
    public Order complete(String commandId,String id,String actor,long version,String auth){return transitions.complete(commandId,id,actor,version,auth);}
    public Order cancel(String commandId,String id,String actor,long version,String auth){return transitions.cancel(commandId,id,actor,version,auth);}
}
