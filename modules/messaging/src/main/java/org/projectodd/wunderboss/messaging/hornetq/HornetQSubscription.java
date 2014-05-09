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

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Subscription;

import javax.jms.Session;
import javax.jms.Topic;

public class HornetQSubscription implements Subscription {

    public HornetQSubscription(HornetQMessaging broker,
                               Endpoint endpoint,
                               String name,
                               String selector) {
        this.broker = broker;
        this.endpoint = (HornetQEndpoint)endpoint;
        this.name = name;
        this.selector = selector;
    }

    @Override
    public Endpoint endpoint() {
        return this.endpoint;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String selector() {
        return this.selector;
    }

    @Override
    public void close() throws Exception {
        if (this.started) {
            Options<Messaging.CreateConnectionOption> options = new Options<>();
            options.put(Messaging.CreateConnectionOption.SUBSCRIPTION, this);
            try (
                    HornetQConnection connection = (HornetQConnection)this.broker.createConnection(options);
                    Session session = connection.jmsConnection().createSession();
            ) {
                session.unsubscribe(this.name);
            }
            this.started = false;
        }
    }

    public HornetQSubscription start() throws Exception {
        if (!this.started) {
            if (this.endpoint.destination() instanceof Topic) {
                Options<Messaging.CreateConnectionOption> options = new Options<>();
                options.put(Messaging.CreateConnectionOption.SUBSCRIPTION, this);
                try (
                        HornetQConnection connection = (HornetQConnection)this.broker.createConnection(options);
                        Session session = connection.jmsConnection().createSession();
                ) {
                    session.createDurableSubscriber((Topic)this.endpoint.destination(),
                                                    this.name, this.selector, false);
                }
            } else {
                log.warn("Subscription requested for a non-Topic destination, ignoring");
            }
            this.started = true;
        }

        return this;
    }

    private final HornetQMessaging broker;
    private final HornetQEndpoint endpoint;
    private final String name;
    private final String selector;
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
