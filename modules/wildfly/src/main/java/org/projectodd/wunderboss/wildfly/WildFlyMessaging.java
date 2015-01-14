/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.wildfly;

import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.JMSQueueService;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.as.messaging.jms.JMSTopicService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.hornetq.HQDestination;
import org.projectodd.wunderboss.messaging.hornetq.HQMessaging;
import org.slf4j.Logger;

import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;

public class WildFlyMessaging extends HQMessaging {

    public WildFlyMessaging(String name, WildFlyService service, Options<CreateOption> options) {
        super(name, options);
        this.wildFlyService = service;
        this.context = service.namingContext();
    }

    @Override
    public synchronized void start() throws Exception {
        started = true;
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            closeCloseables();
            started = false;
        }
    }

    @Override
    protected Queue createQueue(final String name, final String selector, final boolean durable) throws Exception {
        Queue queue = (Queue)waitForValueAvailabilityChange(
                JMSQueueService.installService(null, null, name, this.wildFlyService.serviceTarget(),
                                               hqServiceName(),
                                               selector, durable, new String[]{HQDestination.jndiName(name, "queue")}),
                false);

        if (queue == null) {
            throwTimeout("creation of queue " + name);
        }

        return queue;
    }

    @Override
    protected Topic createTopic(final String name) throws Exception {
        Topic topic =
                (Topic)waitForValueAvailabilityChange(
                        JMSTopicService.installService(null, null, name, hqServiceName(),
                                                       this.wildFlyService.serviceTarget(),
                                                       new String[]{HQDestination.jndiName(name, "topic")}),
                        false);

        if (topic == null) {
            throwTimeout("creation of topic " + name);
        }

        return topic;
    }

    @Override
    protected void destroyQueue(final String name) {
        ServiceController controller = this.wildFlyService.serviceRegistry()
                .getService(JMSServices.getJmsQueueBaseServiceName(hqServiceName()).append(name));
        controller.setMode(ServiceController.Mode.REMOVE);

        if (waitForValueAvailabilityChange(controller, true) != null) {
            throwTimeout("removal of queue " + name);
        }
    }

    @Override
    protected void destroyTopic(final String name) {
        ServiceController controller = this.wildFlyService.serviceRegistry()
                .getService(JMSServices.getJmsTopicBaseServiceName(hqServiceName()).append(name));
        controller.setMode(ServiceController.Mode.REMOVE);

        if (waitForValueAvailabilityChange(controller, true) != null) {
            throwTimeout("removal of topic" + name);
        }
    }

    private void throwTimeout(String message) {
        throw new RuntimeException("Gave up waiting for " + message + " after " +
                valueChangeTimeout() + "ms. If that time is too short, you can adjust with the " +
                TIMEOUT_PROP + " system property.");
    }

    private ServiceName hqServiceName() {
        return MessagingServices.getHornetQServiceName("default");
    }

    private Object waitForValueAvailabilityChange(Value value, boolean toNull) {
        Object v = value.getValue();
        long nap = 10;
        long count = valueChangeTimeout() / nap;
        while ((toNull ? v != null : v == null)
                && count > 0) {
            try { Thread.sleep(nap); } catch (InterruptedException ignored) {}
            count--;
            v = value.getValue();
        }

        return v;
    }

    private long valueChangeTimeout() {
        String timeout = System.getProperty(TIMEOUT_PROP);
        if (timeout != null) {
            return Long.parseLong(timeout);
        } else {
            return 60000;
        }
    }

    @Override
    protected Object lookupJNDI(String jndiName) {
        return lookupJNDIWithRetry(jndiName, 0);
    }

    private Object lookupJNDIWithRetry(String jndiName, int attempt) {
        try {
            return context.lookup(jndiName);
        } catch (NamingException ex) {
            if (ex.getCause() instanceof IllegalStateException
                    && attempt < 100) {
                //TODO: do this a better way
                //the destination isn't yet available
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}

                return lookupJNDIWithRetry(jndiName, attempt + 1);
            }

            return null;
        }
    }

        private final WildFlyService wildFlyService;
    private final Context context;

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.wildfly");
    private final static String TIMEOUT_PROP = "wunderboss.messaging.destination-availability-timeout";
}
