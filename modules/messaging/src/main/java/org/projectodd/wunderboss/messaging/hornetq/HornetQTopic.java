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

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Session;
import org.projectodd.wunderboss.messaging.Topic;

import javax.jms.JMSConsumer;
import java.util.HashMap;
import java.util.Map;

public class HornetQTopic extends HornetQDestination implements Topic {

    public HornetQTopic(javax.jms.Topic topic, HornetQMessaging broker) {
        super(topic, broker);
    }

    @Override
    public Listener subscribe(final String id, final MessageHandler handler,
                              final Map<SubscribeOption, Object> options) throws Exception {
        Options<SubscribeOption> opts = new Options<>(options);
        final HornetQConnection connection = connection(id, opts.get(SubscribeOption.CONNECTION));
        final boolean shouldCloseConnection = !opts.has(SubscribeOption.CONNECTION);
        final HornetQSession session = session(connection);
        final JMSConsumer consumer = session.context().createDurableConsumer((javax.jms.Topic)this.destination(),
                                                                             id,
                                                                             opts.getString(SubscribeOption.SELECTOR), false);

        final Listener listener = new TransactionalListener(handler,
                                                      this,
                                                      connection,
                                                      session,
                                                      consumer,
                                                      false,
                                                      //TODO: a transaction manager
                                                      null).start();

        connection.addCloseable(listener);

        return new Listener() {
            @Override
            public void close() throws Exception {
                if (shouldCloseConnection) {
                    connection.close();
                } else {
                    listener.close();
                }
            }
        };
    }

    @Override
    public void unsubscribe(String id, Map<UnsubscribeOption, Object> options) throws Exception {
        final Options<UnsubscribeOption> opts = new Options<>(options);
        HornetQConnection connection = connection(id, opts.get(UnsubscribeOption.CONNECTION));
        HornetQSession session = null;
        try {
            session = session(connection);
            session.context().unsubscribe(id);
        } finally {
            if (!opts.has(UnsubscribeOption.CONNECTION)) {
                 connection.close();
            } else {
                if (session != null) {
                    session.close();
                }
            }
        }
    }


    protected HornetQConnection connection(final String id, Object connection) throws Exception {
        if (connection == null) {
            connection = broker().createConnection(new HashMap<Messaging.CreateConnectionOption, Object>() {{
                put(Messaging.CreateConnectionOption.CLIENT_ID, id);
            }});
        }

        return (HornetQConnection)connection;
    }

    protected HornetQSession session(HornetQConnection connection) throws Exception {
        return (HornetQSession)connection.createSession(new HashMap<Connection.CreateSessionOption, Object>() {{
            put(Connection.CreateSessionOption.MODE, Session.Mode.TRANSACTED);
        }});
    }
}
