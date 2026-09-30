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
    private final OrderRepository orders; private final CheckpointRepository checkpoints; private final OrderRepostService reposts;
    public LifecycleProcessingService(OrderRepository orders,CheckpointRepository checkpoints,OrderRepostService reposts){this.orders=orders;this.checkpoints=checkpoints;this.reposts=reposts;}
    @Transactional public int expireDue(Instant now){List<Order> due=orders.findByStatusAndExpiresAtLessThanEqual(OrderStatus.OPEN,now);for(Order o:due){o.expire(o.getVersion(),now);orders.save(o);checkpoints.save(new sg.edu.nus.foc.order.domain.OrderCheckpoint(o.getId(),o.getStatus(),now,"lifecycle",null));}return due.size();}
    @Transactional public int repostDue(Instant now){int n=0;for(Order o:orders.findByStatusAndExpiresAtLessThanEqual(OrderStatus.EXPIRED,now)){if(o.eligibleForAutomaticRepost(now)){reposts.automatic("AUTO_REPOST:"+o.getId(),o.getId(),now);n++;}}return n;}
}
