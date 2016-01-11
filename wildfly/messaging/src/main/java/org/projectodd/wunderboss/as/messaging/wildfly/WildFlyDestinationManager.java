/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.as.messaging.wildfly;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.projectodd.wunderboss.as.messaging.ASDestinationManager;
import org.projectodd.wunderboss.as.ASUtils;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;

import javax.jms.Queue;
import javax.jms.Topic;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WildFlyDestinationManager extends ASDestinationManager {

    public WildFlyDestinationManager(ServiceTarget target, ServiceName hqServiceName) {
        super(target, hqServiceName);
    }

    @Override
    public Queue installQueueService(String name, String jndiName,
                                       String selector, boolean durable) throws Exception {
        Queue queue = (Queue) waitForValueAvailabilityChange(
                installService(ASUtils.queueServiceClass(), name, target(),
                               hqServiceName(),
                               selector, durable,
                               new String[]{jndiName}),
                false);

        if (queue == null) {
            throwTimeout("creation of queue " + name);
        }

        return queue;
    }

    @Override
    public Topic installTopicService(String name, String jndiName) throws Exception {
        Topic topic = (Topic) waitForValueAvailabilityChange(
                installService(ASUtils.topicServiceClass(), name, hqServiceName(),
                               target(),
                               new String[]{jndiName}),
                false);

        if (topic == null) {
            throwTimeout("creation of topic " + name);
        }

        return topic;
    }

    @Override
    public void removeDestination(Value service, String name, String jndiName, JMSDestination.Type type) {
        if (waitForValueAvailabilityChange(service, true) != null) {
            throwTimeout("removal of " + type.name + " " + name);
        }
    }

    private Object waitForValueAvailabilityChange(Value value, boolean toNull) {
        Object v = value.getValue();
        long nap = 10;
        long count = destinationServiceTimeout() / nap;
        while ((toNull ? v != null : v == null)
                && count > 0) {
            try { Thread.sleep(nap); } catch (InterruptedException ignored) {}
            count--;
            v = value.getValue();
        }

        return v;
    }

    private Service installService(Class clazz, Object... args) throws Exception {
        Method installer = null;
        for (Method m: clazz.getMethods()) {
            if ("installService".equals(m.getName())) {
                installer = m;
                break;
            }
        }
        if (installer == null) {
            throw new NullPointerException("Class " + clazz + " has no installService method");
        }
        if (installer.getParameterTypes().length == args.length) {
            return (Service) installer.invoke(null, args);
        } else {
            List extra = new ArrayList(Collections.nCopies(2, null));
            extra.addAll(Arrays.asList(args));
            return (Service) installer.invoke(null, extra.toArray());
        }
    }

}
