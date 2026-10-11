package sg.edu.nus.foc.order.messagingpublisher.interfaces;

import sg.edu.nus.foc.order.messagingpublisher.dto.AcceptedOrderCancellationTaskEvent;

public interface IAcceptedOrderCancellationTaskPublisher {
    void publishAcceptedOrderCancellationTask(AcceptedOrderCancellationTaskEvent event);
}
