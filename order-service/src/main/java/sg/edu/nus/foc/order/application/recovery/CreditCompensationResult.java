package sg.edu.nus.foc.order.application.recovery;

/** Only a trusted transport may supply this committed Credit result. No public receiver yet. */
public record CreditCompensationResult(String eventId, String compensationId, String commandId,
        String operation, String orderId, long generation, String outcome, String code) { }
