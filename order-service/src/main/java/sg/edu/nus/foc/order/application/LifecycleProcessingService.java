package sg.edu.nus.foc.order.application;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.foc.order.domain.Order;
import sg.edu.nus.foc.order.domain.OrderStatus;
import sg.edu.nus.foc.order.infrastructure.OrderRepository;
import sg.edu.nus.foc.order.infrastructure.CheckpointRepository;

@Service
public class LifecycleProcessingService {
    private final OrderRepository orders; private final CheckpointRepository checkpoints; private final OrderRepostService reposts; private final CreditServicePort credits; private final OrderAuditLogger audit;
    public LifecycleProcessingService(OrderRepository orders,CheckpointRepository checkpoints,OrderRepostService reposts,CreditServicePort credits,OrderAuditLogger audit){this.orders=orders;this.checkpoints=checkpoints;this.reposts=reposts;this.credits=credits;this.audit=audit;}
    @Transactional public int expireDue(Instant now,String lifecycleAuthorization){List<Order> due=orders.findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull(OrderStatus.OPEN,now);for(Order o:due){long version=o.getVersion();o.expire(version,now);credits.release("EXPIRE:"+o.getId(),o.getId(),o.getRequesterId(),o.getOfferedCredits(),"EXPIRED",version,lifecycleAuthorization);audit.dependency("credit-service", "release", o.getId(), "accepted");orders.save(o);checkpoints.save(new sg.edu.nus.foc.order.domain.OrderCheckpoint(o.getId(),o.getStatus(),now,"lifecycle",null));audit.action("EXPIRE", o.getId(), "lifecycle", "EXPIRE:"+o.getId(), "accepted");}return due.size();}
    @Transactional public int repostDue(Instant now,String lifecycleAuthorization){int n=0;for(Order o:orders.findByStatusAndExpiresAtLessThanEqualAndCourierIdIsNull(OrderStatus.EXPIRED,now)){if(o.eligibleForAutomaticRepost(now)){reposts.automatic("AUTO_REPOST:"+o.getId(),o.getId(),now,lifecycleAuthorization);audit.action("AUTO_REPOST", o.getId(), o.getRequesterId(), "AUTO_REPOST:"+o.getId(), "accepted");n++;}}return n;}
}
