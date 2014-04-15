package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Component;

import javax.jms.Connection;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.Map;

public interface Messaging<T> extends Component<T> {
    enum CreateOption {
        SOME_OPT("some_opt");

        CreateOption(String value) {
            this.value = value;
        }

        public String value;
    }

    enum CreateQueueOption {
        SELECTOR("selector"),
        DURABLE("durable");

        CreateQueueOption(String value) {
            this.value = value;
        }

        public String value;
    }

    //TODO: remote connections?
    Connection createConnection() throws Exception;

    Queue findOrCreateQueue(String name,
                            Map<CreateQueueOption, Object> options) throws Exception;

    Topic findOrCreateTopic(String name) throws Exception;

    boolean releaseQueue(String name, boolean forceConsumers) throws Exception;

    boolean releaseTopic(String name, boolean forceConsumers) throws Exception;
}
