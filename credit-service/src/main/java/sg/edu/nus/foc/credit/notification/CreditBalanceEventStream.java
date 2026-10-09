/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Code generation.
 * Scope: Generated credit service event stream based on provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.notification;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sg.edu.nus.foc.credit.error.CreditStreamUnavailableException;

@Component
public class CreditBalanceEventStream {

    static final long EMITTER_TIMEOUT_MILLIS = 55 * 60 * 1000L;

    private static final Logger log = LoggerFactory.getLogger(CreditBalanceEventStream.class);

    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final LongFunction<SseEmitter> emitterFactory;

    public CreditBalanceEventStream() {
        this(SseEmitter::new);
    }

    CreditBalanceEventStream(LongFunction<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = emitterFactory.apply(EMITTER_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of()));
            return emitter;
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
            throw new CreditStreamUnavailableException(exception);
        }
    }

    public void notifyBalanceChanged(String userId) {
        Set.copyOf(emitters.getOrDefault(userId, Set.of()))
                .forEach(emitter -> send(userId, emitter,
                        SseEmitter.event().name("balance-changed").data(Map.of())));
    }

    @Scheduled(fixedRate = 20_000)
    void heartbeat() {
        emitters.forEach((userId, subscribers) ->
                Set.copyOf(subscribers).forEach(emitter ->
                        send(userId, emitter, SseEmitter.event().comment("keepalive"))));
    }

    int subscriberCount(String userId) {
        return emitters.getOrDefault(userId, Set.of()).size();
    }

    private void send(String userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            log.atDebug()
                    .addKeyValue("service", "credit-service")
                    .addKeyValue("operation", "send_credit_balance_notification")
                    .addKeyValue("userId", userId)
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("credit_balance_notification_connection_removed");
            remove(userId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (ignored, subscribers) -> {
            subscribers.remove(emitter);
            return subscribers.isEmpty() ? null : subscribers;
        });
    }
}
