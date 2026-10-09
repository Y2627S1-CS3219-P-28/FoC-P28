/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */

package sg.edu.nus.foc.credit.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sg.edu.nus.foc.credit.error.CreditStreamUnavailableException;

class CreditBalanceEventStreamTest {

    @Test
    void isolatesSubscribersAndSendsHeartbeats() throws Exception {
        SseEmitter first = mock(SseEmitter.class);
        SseEmitter second = mock(SseEmitter.class);
        Queue<SseEmitter> emitters = new ArrayDeque<>();
        emitters.add(first);
        emitters.add(second);
        CreditBalanceEventStream stream = new CreditBalanceEventStream(ignored -> emitters.remove());

        stream.subscribe("user-a");
        stream.subscribe("user-b");
        verify(first).send(any(SseEmitter.SseEventBuilder.class));
        verify(second).send(any(SseEmitter.SseEventBuilder.class));
        clearInvocations(first, second);

        stream.notifyBalanceChanged("user-a");
        verify(first).send(any(SseEmitter.SseEventBuilder.class));
        verify(second, never()).send(any(SseEmitter.SseEventBuilder.class));

        clearInvocations(first, second);
        stream.heartbeat();
        verify(first).send(any(SseEmitter.SseEventBuilder.class));
        verify(second).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void removesCompletedTimedOutAndFailedEmitters() throws Exception {
        SseEmitter completed = mock(SseEmitter.class);
        SseEmitter timedOut = mock(SseEmitter.class);
        SseEmitter errored = mock(SseEmitter.class);
        SseEmitter failed = mock(SseEmitter.class);
        AtomicReference<Runnable> completion = captureCompletion(completed);
        AtomicReference<Runnable> timeout = captureTimeout(timedOut);
        AtomicReference<Consumer<Throwable>> error = captureError(errored);
        Queue<SseEmitter> emitters = new ArrayDeque<>();
        emitters.add(completed);
        emitters.add(timedOut);
        emitters.add(errored);
        emitters.add(failed);
        CreditBalanceEventStream stream = new CreditBalanceEventStream(ignored -> emitters.remove());

        stream.subscribe("user-a");
        stream.subscribe("user-a");
        stream.subscribe("user-a");
        doNothing().doThrow(new IOException("disconnected"))
                .when(failed).send(any(SseEmitter.SseEventBuilder.class));
        stream.subscribe("user-a");
        assertThat(stream.subscriberCount("user-a")).isEqualTo(4);

        completion.get().run();
        timeout.get().run();
        error.get().accept(new IOException("client disconnected"));
        stream.notifyBalanceChanged("user-a");

        assertThat(stream.subscriberCount("user-a")).isZero();
        verify(failed).completeWithError(any(IOException.class));
    }

    @Test
    void rejectsASubscriptionWhenTheInitialSendFails() throws Exception {
        SseEmitter failed = mock(SseEmitter.class);
        doThrow(new IOException("cannot send"))
                .when(failed).send(any(SseEmitter.SseEventBuilder.class));
        CreditBalanceEventStream stream = new CreditBalanceEventStream(ignored -> failed);

        assertThatThrownBy(() -> stream.subscribe("user-a"))
                .isInstanceOf(CreditStreamUnavailableException.class);
        assertThat(stream.subscriberCount("user-a")).isZero();
        verify(failed).completeWithError(any(IOException.class));
    }

    private static AtomicReference<Runnable> captureCompletion(SseEmitter emitter) {
        AtomicReference<Runnable> callback = new AtomicReference<>();
        doAnswer(invocation -> {
            callback.set(invocation.getArgument(0));
            return null;
        }).when(emitter).onCompletion(any(Runnable.class));
        return callback;
    }

    private static AtomicReference<Runnable> captureTimeout(SseEmitter emitter) {
        AtomicReference<Runnable> callback = new AtomicReference<>();
        doAnswer(invocation -> {
            callback.set(invocation.getArgument(0));
            return null;
        }).when(emitter).onTimeout(any(Runnable.class));
        return callback;
    }

    private static AtomicReference<Consumer<Throwable>> captureError(SseEmitter emitter) {
        AtomicReference<Consumer<Throwable>> callback = new AtomicReference<>();
        doAnswer(invocation -> {
            callback.set(invocation.getArgument(0));
            return null;
        }).when(emitter).onError(any());
        return callback;
    }
}
