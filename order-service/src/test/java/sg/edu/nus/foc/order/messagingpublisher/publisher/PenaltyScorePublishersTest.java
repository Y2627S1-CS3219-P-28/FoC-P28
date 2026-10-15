package sg.edu.nus.foc.order.messagingpublisher.publisher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import sg.edu.nus.foc.order.messagingpublisher.dto.PenaltyScoreAdditionEvent;
import sg.edu.nus.foc.order.messagingpublisher.dto.PenaltyScoreDeductionEvent;
import sg.edu.nus.foc.order.messagingpublisher.interfaces.IEventPublisher;

class PenaltyScorePublishersTest {

    @Test
    void routesToConfiguredTopicsAndPropagatesTransportFailure() {
        @SuppressWarnings("unchecked")
        IEventPublisher<Object> transport = mock(IEventPublisher.class);
        PenaltyScoreAdditionPublisher addition = new PenaltyScoreAdditionPublisher(transport);
        PenaltyScoreDeductionPublisher deduction = new PenaltyScoreDeductionPublisher(transport);
        ReflectionTestUtils.setField(addition, "topicId", "courier-penalty-addition-dev-v1");
        ReflectionTestUtils.setField(deduction, "topicId", "courier-penalty-deduction-dev-v1");
        PenaltyScoreAdditionEvent added = new PenaltyScoreAdditionEvent();
        PenaltyScoreDeductionEvent deducted = new PenaltyScoreDeductionEvent();
        addition.publishPenaltyScoreAddition(added);
        deduction.publishPenaltyScoreDeduction(deducted);
        verify(transport).publish("courier-penalty-addition-dev-v1", added);
        verify(transport).publish("courier-penalty-deduction-dev-v1", deducted);
        doThrow(new EventPublicationException("unavailable")).when(transport)
                .publish("courier-penalty-addition-dev-v1", added);
        assertThrows(EventPublicationException.class, () -> addition.publishPenaltyScoreAddition(added));
    }
}
