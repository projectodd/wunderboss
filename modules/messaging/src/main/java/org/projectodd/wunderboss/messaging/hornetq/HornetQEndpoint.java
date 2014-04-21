package org.projectodd.wunderboss.messaging.hornetq;

import org.hornetq.jms.server.JMSServerManager;
import org.projectodd.wunderboss.messaging.jms.DestinationEndpoint;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

public class HornetQEndpoint extends DestinationEndpoint {

    public HornetQEndpoint(Destination dest, JMSServerManager jmsServerManager, boolean durable) {
        super(dest, durable);
        this.jmsServerManager = jmsServerManager;
    }

    @Override
    public void close() throws Exception {
        if (!this.closed) {
            Destination dest = this.implementation();
            if (dest instanceof Queue) {
                this.jmsServerManager.destroyQueue(((Queue) dest).getQueueName(), true);
            } else {
                this.jmsServerManager.destroyTopic(((Topic)dest).getTopicName(), true);
            }

            this.closed = true;
        }
    }

    private boolean closed = false;
    private final JMSServerManager jmsServerManager;
}
