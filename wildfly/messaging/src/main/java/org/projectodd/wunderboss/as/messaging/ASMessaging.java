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

package org.projectodd.wunderboss.as.messaging;

import org.jboss.msc.service.ServiceController;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.as.ASUtils;
import org.projectodd.wunderboss.as.WunderBossService;
import org.projectodd.wunderboss.messaging.hornetq.HQMessaging;
import org.projectodd.wunderboss.messaging.jms.DestinationUtil;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;
import org.slf4j.Logger;

import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;

public class ASMessaging extends HQMessaging {

    public ASMessaging(String name, WunderBossService service,
                       ASDestinationManager destinationManager, Options<CreateOption> options) {
        super(name, options);
        this.mscService = service;
        this.context = service.namingContext();
        this.destinationManager = destinationManager;
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
        return this.destinationManager.installQueueService(name,
                                                           DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE),
                                                           selector, durable);
    }

    @Override
    protected Topic createTopic(final String name) throws Exception {
        return this.destinationManager.installTopicService(name,
                                                           DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC));
    }

    @Override
    protected void destroyQueue(final String name) {
        ServiceController controller = this.mscService.serviceRegistry().getService(ASUtils.queueServiceName(name));
        controller.setMode(ServiceController.Mode.REMOVE);

        this.destinationManager.removeDestination(controller, name,
                                                  DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE),
                                                  JMSDestination.Type.QUEUE);
    }

    @Override
    protected void destroyTopic(final String name) {
        ServiceController controller = this.mscService.serviceRegistry().getService(ASUtils.topicServiceName(name));
        controller.setMode(ServiceController.Mode.REMOVE);

        this.destinationManager.removeDestination(controller, name,
                                                  DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC),
                                                  JMSDestination.Type.TOPIC);
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

    private final WunderBossService mscService;
    private final Context context;
    private final ASDestinationManager destinationManager;

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.as");

}
