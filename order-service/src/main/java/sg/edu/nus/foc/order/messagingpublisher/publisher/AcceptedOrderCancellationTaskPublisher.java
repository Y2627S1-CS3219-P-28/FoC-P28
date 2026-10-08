package sg.edu.nus.foc.order.messagingpublisher.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IAcceptedOrderCancellationTaskPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IEventPublisher;

@Component
@RequiredArgsConstructor
public class AcceptedOrderCancellationTaskPublisher implements IAcceptedOrderCancellationTaskPublisher {
    private final IEventPublisher<Object> eventPublisher;

    @Value("${order.messaging.topics.accepted-order-cancellation:TODO_TOPIC}")
    private String topicId = "TODO_TOPIC";

    @Override
    public void publishAcceptedOrderCancellationTask(AcceptedOrderCancellationTaskEvent event) {
        eventPublisher.publish(topicId, event);
    }
}
