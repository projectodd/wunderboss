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
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Session;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import java.util.Collections;
import java.util.Map;

public abstract class HornetQDestination implements org.projectodd.wunderboss.messaging.Destination {
    public static final String CONTENT_TYPE_PROPERTY = "contentType";

     public HornetQDestination(Destination dest, HornetQMessaging broker) {
        this.destination = dest;
        this.broker = broker;
    }

    public Destination destination() {
        return this.destination;
    }

    public abstract String fullName();

    @Override
    public Listener listen(MessageHandler handler, Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        HornetQConnection connection = connection(opts.get(ListenOption.CONNECTION));
        Listener listener = new MessageHandlerGroup(connection, handler,
                                                    this,
                                                    opts).start();
        connection.addCloseable(listener);
        this.broker.addCloseableForDestination(this, listener);

        return listener;
    }

    @Override
    public void send(String content, String contentType,
                     Map<MessageOpOption, Object> options) throws Exception {
        _send(content, contentType, options);
    }

    @Override
    public void send(byte[] content, String contentType,
                     Map<MessageOpOption, Object> options) throws Exception {
        _send(content, contentType, options);
    }

    protected static void fillInProperties(JMSProducer producer, Map<String, Object> properties) throws JMSException {
        for(Map.Entry<String, Object> each : properties.entrySet()) {
            producer.setProperty(each.getKey(), each.getValue());
        }
    }

    protected Session getSession(Options<MessageOpOption> options) throws Exception {
        Session session = (Session)options.get(MessageOpOption.SESSION);
        if (session == null) {
            session = connection(options.get(MessageOpOption.CONNECTION)).createSession(null);
        }

        return session;
    }

    protected HornetQConnection connection(Object connection) throws Exception {
        if (connection == null) {
            connection = this.broker.defaultConnection();
        }

        return (HornetQConnection)connection;
    }

    protected void _send(Object message, String contentType,
                         Map<MessageOpOption, Object> options) throws Exception {
        _send(message, contentType, options, Collections.EMPTY_MAP);
    }

    protected void _send(Object message, String contentType,
                Map<MessageOpOption, Object> options, Map<String, Object> additionalProperties) throws Exception {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType can't be null");
        }
        Options<MessageOpOption> opts = new Options<>(options);
        boolean closeSession = !opts.has(MessageOpOption.SESSION);
        Session session = getSession(opts);

        try {
            JMSProducer producer = ((HornetQSession)session).context().createProducer();
            fillInProperties(producer, (Map<String, Object>) opts.get(SendOption.PROPERTIES, Collections.emptyMap()));
            fillInProperties(producer, additionalProperties);
            producer
                    .setProperty(CONTENT_TYPE_PROPERTY, contentType)
                    .setDeliveryMode((opts.getBoolean(SendOption.PERSISTENT) ?
                            DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT))
                    .setPriority(opts.getInt(SendOption.PRIORITY))
                    .setTimeToLive(opts.getLong(SendOption.TTL, producer.getTimeToLive()));
            if (message instanceof String) {
                producer.send(this.destination, (String)message);
            } else {
                producer.send(this.destination, (byte[])message);
            }
        } finally {
            if (closeSession) {
                session.close();
            }
        }
    }

    @Override
    public Message receive(Map<MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        int timeout = opts.getInt(ReceiveOption.TIMEOUT);
        boolean closeSession = !opts.has(MessageOpOption.SESSION);
        Session session = getSession(opts);
        try {
            String selector = opts.getString(ReceiveOption.SELECTOR);
            JMSConsumer consumer = ((HornetQSession)session).context().createConsumer(this.destination, selector);

            javax.jms.Message message;
            if (timeout == -1) {
                message = consumer.receiveNoWait();
            } else {
                message = consumer.receive(timeout);
            }

            if (message != null) {
                return new HornetQMessage(message, this, connection(opts.get(ReceiveOption.CONNECTION)));
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

    private final Destination destination;
    private boolean stopped = false;
    private final HornetQMessaging broker;
}
