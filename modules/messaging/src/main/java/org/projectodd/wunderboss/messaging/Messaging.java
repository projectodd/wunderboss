package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Component;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.Map;

public interface Messaging<T> extends Component<T> {

    enum CreateOption {
        /**
         * Specifies if xa is on by default. Defaults to false.
         */
        XA("xa");

        CreateOption(String value) {
            this.value = value;
        }

        public String value;
    }

    enum CreateQueueOption {
        DURABLE("durable"),
        SELECTOR("selector");

        CreateQueueOption(String value) {
            this.value = value;
        }

        public String value;
    }

    Queue findOrCreateQueue(String name,
                            Map<CreateQueueOption, Object> options) throws Exception;

    Topic findOrCreateTopic(String name) throws Exception;

    boolean releaseQueue(String name, boolean forceConsumers) throws Exception;

    boolean releaseQueue(Queue destination, boolean forceConsumers) throws Exception;

    boolean releaseTopic(String name, boolean forceConsumers) throws Exception;

    boolean releaseTopic(Topic destination, boolean forceConsumers) throws Exception;

    enum CreateConnectionOption {
        /**
         * If true, and xa connection is returned. Defaults to whatever was specified for
         * CreateOption.XA.
         */
        XA("xa");

        CreateConnectionOption(String value) {
            this.value = value;
        }

        public String value;
    }

    //TODO: remote connections?
    Connection createConnection(Map<CreateConnectionOption, Object> options) throws Exception;

    enum ListenOption {
        /**
         * The client-id for durable topic subscriptions. Ignored for queues.
         */
        CLIENT_ID("client_id"),
        CONCURRENCY("concurrency"),
        DURABLE("durable"),
        /**
         * ID used when storing a ref to the listener for unlisten. Defaults to a UUID.
         */
        LISTENER_ID("listener_id"),
        SELECTOR("selector"),
        /**
         * subscriber-name for durable topic subscriptions. Ignored for queues. Defaults to
         * LISTENER_ID, then falls back to CLIENT_ID if LISTENER_ID not set.
         */
        SUBSCRIBER_NAME("subscriber_name"),
        /**
         * If true, and xa is used around the onMessage(). Defaults to whatever was specified for
         * CreateOption.XA.
         */
        XA("xa");

        ListenOption(String value) {
            this.value = value;
        }

        public String value;
    }

    String listen(Destination destination, MessageListener listener,
                  Map<ListenOption, Object> options) throws Exception;

    boolean unlisten(String id) throws Exception;

    boolean isXaDefault();
}
