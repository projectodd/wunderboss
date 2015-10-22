/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.as.messaging;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;

import javax.jms.Queue;
import javax.jms.Topic;

public abstract class ASDestinationManager {
    final static String TIMEOUT_PROP = "wunderboss.messaging.destination-availability-timeout";

    public ASDestinationManager(ServiceTarget target, ServiceName hqServiceName) {
        this.serviceTarget = target;
        this.hqServiceName = hqServiceName;
    }

    public abstract Queue installQueueService(String name, String jndiName, String selector,
                                                boolean durable) throws Exception;

    public abstract Topic installTopicService(String name, String jndiName) throws Exception;

    public abstract void removeDestination(Value service, String name, String jndiName, JMSDestination.Type type);

    protected ServiceTarget target() {
        return this.serviceTarget;
    }

    protected ServiceName hqServiceName() {
        return this.hqServiceName;
    }

    protected long destinationServiceTimeout() {
        String timeout = System.getProperty(TIMEOUT_PROP);
        if (timeout != null) {
            return Long.parseLong(timeout);
        } else {
            return 60000;
        }
    }

    protected void throwTimeout(String message) {
        throw new RuntimeException("Gave up waiting for " + message + " after " +
                                           destinationServiceTimeout() + "ms. If that time is too short, you can adjust with the " +
                                           TIMEOUT_PROP + " system property.");
    }

    private final ServiceTarget serviceTarget;
    private final ServiceName hqServiceName;
}
