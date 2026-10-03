package sg.edu.nus.foc.order.messagingpublisher.publisher;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.cloud.pubsub.v1.Publisher;
import org.junit.jupiter.api.Test;

class GoogleCloudPubSubPublisherFactoryTest {
    @Test
    void reusesPublisherByTopicAndClosesIt() {
        GoogleCloudPubSubPublisherFactory factory = new GoogleCloudPubSubPublisherFactory("localhost:8085");

        Publisher first = factory.forTopic("local-project", "orders");
        Publisher second = factory.forTopic("local-project", "orders");
        Publisher anotherTopic = factory.forTopic("local-project", "other-orders");

        assertSame(first, second);
        assertNotSame(first, anotherTopic);
        factory.close();
    }

    @Test
    void createsAndClosesPublisherWithEmulatorTransport() {
        GoogleCloudPubSubPublisherFactory factory = new GoogleCloudPubSubPublisherFactory("localhost:8085");

        Publisher publisher = factory.forTopic("local-project", "orders");

        assertSame(publisher, factory.forTopic("local-project", "orders"));
        factory.close();
    }
}
