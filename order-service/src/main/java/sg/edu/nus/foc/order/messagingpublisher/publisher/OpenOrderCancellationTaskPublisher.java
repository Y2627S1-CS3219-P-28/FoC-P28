package sg.edu.nus.foc.order.messagingpublisher.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IEventPublisher;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IOpenOrderCancellationTaskPublisher;

@Component
@RequiredArgsConstructor
public class OpenOrderCancellationTaskPublisher implements IOpenOrderCancellationTaskPublisher {
    private final IEventPublisher<Object> eventPublisher;

    @Value("${order.messaging.topics.open-order-cancellation:TODO_TOPIC}")
    private String topicId = "TODO_TOPIC";

    @Override
    public void publishOpenOrderCancellationTask(OpenOrderCancellationTaskEvent event) {
        eventPublisher.publish(topicId, event);
    }
}
