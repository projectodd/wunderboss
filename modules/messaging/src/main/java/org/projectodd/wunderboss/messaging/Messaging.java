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

    enum CreateConnectionOption {
        XA("xa");

        CreateConnectionOption(String value) {
            this.value = value;
        }

        public String value;
    }

    enum ListenOption {
        CLIENT_ID("client_id"),
        CONCURRENCY("concurrency"),
        DURABLE("durable"),
        LISTENER_ID("listener_id"),
        SELECTOR("selector"),
        SUBSCRIBER_NAME("subscriber_name"),
        XA("xa");

        ListenOption(String value) {
            this.value = value;
        }

        public String value;
    }

    //TODO: remote connections?
    Connection createConnection(Map<CreateConnectionOption, Object> options) throws Exception;

    Queue findOrCreateQueue(String name,
                            Map<CreateQueueOption, Object> options) throws Exception;

    Topic findOrCreateTopic(String name) throws Exception;

    boolean releaseQueue(String name, boolean forceConsumers) throws Exception;

    boolean releaseTopic(String name, boolean forceConsumers) throws Exception;

    String listen(Destination destination, MessageListener listener,
                  Map<ListenOption, Object> options) throws Exception;

    boolean unlisten(String id) throws Exception;

    boolean isXaDefault();
}
