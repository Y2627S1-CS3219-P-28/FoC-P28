package sg.edu.nus.foc.order.messagingpublisher.publisher;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GoogleCloudPubSubPublisherFactory implements PubSubPublisherFactory {
    private final Function<ProjectTopicName, Publisher> publisherCreator;

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    public GoogleCloudPubSubPublisherFactory() {
        this(GoogleCloudPubSubPublisherFactory::createPublisher);
    }

    GoogleCloudPubSubPublisherFactory(Function<ProjectTopicName, Publisher> publisherCreator) {
        this.publisherCreator = publisherCreator;
    }

    @Override
    public Publisher forTopic(String projectId, String topicId) {
        String publisherKey = projectId + "/" + topicId;
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        return publishers.computeIfAbsent(publisherKey, ignored -> publisherCreator.apply(topicName));
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
    }

    private static Publisher createPublisher(ProjectTopicName topicName) {
        try {
            return Publisher.newBuilder(topicName).build();
        } catch (IOException exception) {
            throw new EventPublicationException("Could not create a Google Pub/Sub publisher.", exception);
        }
    }
}
