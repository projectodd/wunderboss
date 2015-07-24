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

package org.projectodd.wunderboss.as.eap;

import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.projectodd.wunderboss.as.ASDestinationManager;
import org.projectodd.wunderboss.as.ASUtils;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;

import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;

public class EAPDestinationManager extends ASDestinationManager {

    public EAPDestinationManager(ServiceTarget target, ServiceName hqServiceName, Context context) {
        super(target, hqServiceName);
        this.context = context;
    }

    @Override
    public Queue installQueueService(String name, String jndiName,
                                       String selector, boolean durable) throws Exception {
        JMSQueueAdd.INSTANCE.installServices(null, null, name, target(), hqServiceName(), selector,
                                             durable, new String[]{jndiName});

        Queue queue = (Queue) ASUtils.waitForAppearanceInJNDI(this.context, jndiName, destinationServiceTimeout());

        if (queue == null) {
            throwTimeout("creation of queue " + name);
        }

        return queue;
    }

    @Override
    public Topic installTopicService(String name, String jndiName) throws Exception {
        JMSTopicAdd.INSTANCE.installServices(null, null, name, hqServiceName(), target(), new String[]{jndiName});

        Topic topic = (Topic) ASUtils.waitForAppearanceInJNDI(this.context, jndiName, destinationServiceTimeout());

        if (topic == null) {
            throwTimeout("creation of topic " + name);
        }

        return topic;
    }

    @Override
    public void removeDestination(Value service, String name, String jndiName, JMSDestination.Type type) {
        if (!ASUtils.waitForRemovalFromJNDI(this.context, jndiName, destinationServiceTimeout())) {
            throwTimeout("removal of " + type.name + " " + name);
        }
    }

    private final Context context;
}
