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
import org.projectodd.wunderboss.Pair;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Session;
import org.projectodd.wunderboss.messaging.Connection;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public abstract class HornetQDestination implements org.projectodd.wunderboss.messaging.Destination {

    public HornetQDestination(String name, Destination destination, HornetQMessaging broker) {
        this.name = name;
        this.jmsDestination = destination;
        this.broker = broker;
    }

    @Override
    public String name() {
        return this.name;
    }

    public Destination jmsDestination() {
        return this.jmsDestination;
    }

    public abstract String fullName();

    public abstract String jmsName();

    @Override
    public Listener listen(MessageHandler handler, Codecs codecs, Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        Connection connection = connection(opts.get(ListenOption.CONNECTION));
        Listener listener = new MessageHandlerGroup(connection, handler,
                                                    codecs, this,
                                                    opts).start();
        connection.addCloseable(listener);
        this.broker.addCloseableForDestination(this, listener);

        return listener;
    }

    @Override
    public void send(Object content, Codec codec,
                     Map<MessageOpOption, Object> options) throws Exception {
        _send(content, codec, options, Collections.EMPTY_MAP);
    }

    protected static void fillInProperties(JMSProducer producer, Map<String, Object> properties) throws JMSException {
        for(Map.Entry<String, Object> each : properties.entrySet()) {
            producer.setProperty(each.getKey(), each.getValue());
        }
    }

    protected Pair<Session, Boolean> getSession(Options<MessageOpOption> options) throws Exception {
        Session session = (Session)options.get(MessageOpOption.SESSION);
        boolean shouldClose = false;

        if (session == null) {
            Connection connection = connection(options.get(MessageOpOption.CONNECTION));
            Session threadSession = HornetQSession.currentSession.get();

            if (threadSession != null &&
                    threadSession.connection() == connection) {
                session = threadSession;
            } else {
                session = connection.createSession(null);
                shouldClose = true;
            }
        }

        session.enlist();

        return new Pair(session, shouldClose);
    }

    // TODO: we may need to pass options here since we may need to
    // pass through HOST and CLIENT_ID to createConnection
    protected Connection connection(Object connection) throws Exception {
        if (connection == null) {
            return this.broker.defaultConnection();
        }
        if (connection == Connection.XA) {
            return this.broker.createConnection(new HashMap() {{
                put(Messaging.CreateConnectionOption.XA, true);
            }});
        }
        HornetQConnection c = (HornetQConnection) connection;
        return c.new NonClosing();
    }

    protected void _send(Object message, Codec codec,
                Map<MessageOpOption, Object> options, Map<String, Object> additionalProperties) throws Exception {
        if (codec == null) {
            throw new IllegalArgumentException("codec can't be null");
        }
        Options<MessageOpOption> opts = new Options<>(options);
        Pair<Session, Boolean> sessionInfo = getSession(opts);
        Session session = sessionInfo.first;
        boolean closeSession = sessionInfo.second;
        JMSContext context = ((HornetQSession)session).context();
        javax.jms.Destination destination = jmsDestination();

        try {
            JMSProducer producer = context.createProducer();
            fillInProperties(producer, (Map<String, Object>) opts.get(SendOption.PROPERTIES, Collections.emptyMap()));
            fillInProperties(producer, additionalProperties);
            producer
                    .setProperty(HornetQMessage.CONTENT_TYPE_PROPERTY, codec.contentType())
                    .setDeliveryMode((opts.getBoolean(SendOption.PERSISTENT) ?
                            DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT))
                    .setPriority(opts.getInt(SendOption.PRIORITY))
                    .setTimeToLive(opts.getLong(SendOption.TTL, producer.getTimeToLive()));
            Object encoded = codec.encode(message);
            Class encodesTo = codec.encodesTo();

            if (encodesTo == String.class) {
                producer.send(destination, (String)encoded);
            } else if (encodesTo == byte[].class) {
                producer.send(destination, (byte[])encoded);
            } else {
                producer.send(destination, (Serializable)encoded);
            }

        } finally {
            if (closeSession) {
                session.close();
            }
        }
    }

    @Override
    public Message receive(Codecs codecs, Map<MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        int timeout = opts.getInt(ReceiveOption.TIMEOUT);
        Pair<Session, Boolean> sessionInfo = getSession(opts);
        Session session = sessionInfo.first;
        boolean closeSession = sessionInfo.second;
        JMSContext context = ((HornetQSession)session).context();
        javax.jms.Destination destination = jmsDestination();
        String selector = opts.getString(ReceiveOption.SELECTOR);

        try (JMSConsumer consumer = context.createConsumer(destination, selector)) {
            javax.jms.Message message;
            if (timeout == -1) {
                message = consumer.receiveNoWait();
            } else {
                message = consumer.receive(timeout);
            }

            if (message != null) {
                String contentType = HornetQMessage.contentType(message);
                Codec codec = codecs.forContentType(contentType);
                return new HornetQMessage(message, codec, this);
            } else {
                return null;
            }
        } finally {
            if (closeSession) {
                session.close();
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (!this.stopped) {
            this.broker.destroyDestination(this);
            this.stopped = true;
        }
    }

    protected HornetQMessaging broker() {
        return this.broker;
    }


    public static String jndiName(String name, String type) {
        return ("java:/jms/" + type + '/' + name).replace("//", "/_/");
    }

    public
    static boolean isJndiName(String name) {
        return name.startsWith("java:");
    }

    private final String name;
    private final Destination jmsDestination;
    private boolean stopped = false;
    private final HornetQMessaging broker;
}
