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
import org.projectodd.wunderboss.messaging.Connection.ListenOption;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Subscription;
import org.projectodd.wunderboss.messaging.jms.DestinationEndpoint;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XAConnection;
import java.util.ArrayList;
import java.util.List;

public class MessageHandlerGroup implements Listener {

    public MessageHandlerGroup(HornetQConnection connection,
                               MessageHandler handler,
                               Endpoint endpoint,
                               Options<ListenOption> options) {
        this.connection = connection;
        this.handler = handler;
        this.endpoint = (DestinationEndpoint)endpoint;
        this.options = options;
    }

    public synchronized MessageHandlerGroup start() {
        if (!this.started) {
            try {
                int concurrency = this.options.getInt(ListenOption.CONCURRENCY, 1);
                while(concurrency-- > 0) {
                    Session session = createSession();
                    listeners.add(new TransactionalListener(this.handler,
                                                            this.endpoint,
                                                            this.connection,
                                                            session,
                                                            createConsumer(session),
                                                            isXAEnabled(),
                                                            //TODO: a transaction manager
                                                            null).start());
                }

            } catch (Exception e) {
                log.error("Failed to start handler group: ", e);
            }

            this.started = true;
        }

        return this;
    }

    @Override
    public synchronized void close() throws Exception {
        if (this.started) {
            for(TransactionalListener each : this.listeners) {
                each.stop();
            }

            this.listeners.clear();

            this.started = false;
        }
    }

    protected Session createSession() throws JMSException {
        if (isXAEnabled()) {
            return ((XAConnection)this.connection.jmsConnection()).createXASession();
        } else {
            // Use local transactions for non-XA message processors
            //return
            //this.connection.jmsConnection().createSession(true,
            //Session.SESSION_TRANSACTED);
            return this.connection.jmsConnection().createSession();
        }
    }

    protected MessageConsumer createConsumer(Session session) throws JMSException {
        String selector = this.options.getString(ListenOption.SELECTOR);
        Destination destination = endpoint.destination();
        if (isDurable()) {
            Subscription subscription = (Subscription)this.options.get(ListenOption.SUBSCRIPTION);
            return session.createDurableSubscriber((Topic) destination,
                                                   subscription.name(),
                                                   selector, false);
        } else {
            return session.createConsumer(destination, selector);
        }

    }

    protected boolean isXAEnabled() {
        return this.options.getBoolean(ListenOption.XA, this.connection.isXAEnabled());
    }

    protected boolean isDurable() {
        return this.options.has(ListenOption.SUBSCRIPTION) &&
                this.endpoint.destination() instanceof Topic;
    }

    private final MessageHandler handler;
    private final DestinationEndpoint endpoint;
    private final Options<ListenOption> options;
    private HornetQConnection connection;
    private final List<TransactionalListener> listeners = new ArrayList<>();
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
