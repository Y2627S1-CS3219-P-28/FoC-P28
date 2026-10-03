package sg.edu.nus.foc.order.messagingpublisher.publisher;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GoogleCloudPubSubPublisherFactory implements PubSubPublisherFactory {
    private final String emulatorHost;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private final Map<String, ManagedChannel> emulatorChannels = new ConcurrentHashMap<>();

    public GoogleCloudPubSubPublisherFactory(
            @Value("${order.messaging.emulator-host:}") String emulatorHost) {
        this.emulatorHost = emulatorHost;
    }

    @Override
    public Publisher forTopic(String projectId, String topicId) {
        String publisherKey = projectId + "/" + topicId;
        return publishers.computeIfAbsent(publisherKey, ignored -> create(projectId, topicId, publisherKey));
    }

    @PreDestroy
    public void close() {
        publishers.values().forEach(Publisher::shutdown);
        publishers.values().forEach(publisher -> {
            try {
                publisher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while closing a Pub/Sub publisher.");
            }
        });
        emulatorChannels.values().forEach(ManagedChannel::shutdownNow);
    }

    private Publisher create(String projectId, String topicId, String publisherKey) {
        Publisher.Builder builder = Publisher.newBuilder(ProjectTopicName.of(projectId, topicId));
        if (emulatorHost == null || emulatorHost.isBlank()) {
            try {
                return builder.build();
            } catch (IOException exception) {
                throw new EventPublicationException("Could not create a Google Pub/Sub publisher.", exception);
            }
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(emulatorHost)
                .usePlaintext()
                .build();
        try {
            Publisher publisher = builder
                    .setChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            emulatorChannels.put(publisherKey, channel);
            return publisher;
        } catch (IOException | RuntimeException exception) {
            channel.shutdownNow();
            if (exception instanceof EventPublicationException publicationException) {
                throw publicationException;
            }
            throw new EventPublicationException("Could not create a Google Pub/Sub emulator publisher.", exception);
        }
    }
}
