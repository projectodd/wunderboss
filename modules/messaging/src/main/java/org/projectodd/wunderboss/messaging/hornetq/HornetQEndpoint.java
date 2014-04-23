/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            Destination dest = this.destination();
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
