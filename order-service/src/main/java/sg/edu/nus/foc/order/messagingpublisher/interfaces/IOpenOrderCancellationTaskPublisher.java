package sg.edu.nus.foc.order.messagingpublisher.interfaces;

import sg.edu.nus.foc.order.messagingpublisher.dto.OpenOrderCancellationTaskEvent;

public interface IOpenOrderCancellationTaskPublisher {
    void publishOpenOrderCancellationTask(OpenOrderCancellationTaskEvent event);
}
