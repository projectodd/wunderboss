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

package org.projectodd.wunderboss.messaging.jms2;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class JMSDestination implements org.projectodd.wunderboss.messaging.Destination {

    public JMSDestination(String name, Destination destination, JMSMessaging broker) {
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

    public abstract int defaultConcurrency();

    @Override
    public Listener listen(MessageHandler handler, Codecs codecs, Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        Context givenContext = (Context)opts.get(ListenOption.CONTEXT);

        if (givenContext != null &&
                !givenContext.isRemote()) {
            throw new IllegalArgumentException("Listening only accepts a remote context.");
        }

        JMSSpecificContext context = context(givenContext);
        Listener listener = new MessageHandlerGroup(context,
                                                    handler,
                                                    codecs,
                                                    this,
                                                    opts).start();

        if (givenContext != null) {
            givenContext.addCloseable(listener);
        }
        this.broker.addCloseableForDestination(this, listener);
        this.broker.addCloseable(listener);

        return listener;
    }

    @Override
    public void publish(Object content, Codec codec,
                        Map<MessageOpOption, Object> options) throws Exception {
        publish(content, codec, options, Collections.EMPTY_MAP);
    }

    protected static void fillInProperties(JMSProducer producer, Map<String, Object> properties) throws JMSException {
        for(Map.Entry<String, Object> each : properties.entrySet()) {
            producer.setProperty(each.getKey(), each.getValue());
        }
    }

    protected JMSSpecificContext context(final Object context) throws Exception {
        Context newContext;
        JMSSpecificContext threadContext = JMSContext.currentContext.get();

        if (context != null) {
            newContext = ((JMSSpecificContext) context).asNonCloseable();
        } else if (JMSXAContext.isTransactionActive()) {
            newContext = this.broker.createContext(new HashMap() {{
                put(Messaging.CreateContextOption.XA, true);
            }});
        } else if (threadContext != null) {
            newContext = threadContext;
        } else {
            newContext = this.broker.createContext(null);
        }

        newContext.enlist();

        return (JMSSpecificContext)newContext;
    }

    protected void publish(Object message, Codec codec,
                           Map<MessageOpOption, Object> options, Map<String, Object> additionalProperties) throws Exception {
        if (codec == null) {
            throw new IllegalArgumentException("codec can't be null");
        }
        Options<MessageOpOption> opts = new Options<>(options);
        JMSSpecificContext context = context(opts.get(MessageOpOption.CONTEXT));
        javax.jms.JMSContext jmsContext = context.jmsContext();
        javax.jms.Destination destination = jmsDestination();

        try {
            JMSProducer producer = jmsContext.createProducer();
            fillInProperties(producer, (Map<String, Object>) opts.get(PublishOption.PROPERTIES, Collections.emptyMap()));
            fillInProperties(producer, additionalProperties);
            producer
                    .setProperty(JMSMessage.CONTENT_TYPE_PROPERTY, codec.contentType())
                    .setDeliveryMode((opts.getBoolean(PublishOption.PERSISTENT) ?
                            DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT))
                    .setPriority(opts.getInt(PublishOption.PRIORITY))
                    .setTimeToLive(opts.getLong(PublishOption.TTL, producer.getTimeToLive()));
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
            context.close();
        }
    }

    @Override
    public Message receive(Codecs codecs, Map<MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        int timeout = opts.getInt(ReceiveOption.TIMEOUT);
        JMSSpecificContext context = context(opts.get(MessageOpOption.CONTEXT));
        javax.jms.JMSContext jmsContext = context.jmsContext();
        javax.jms.Destination destination = jmsDestination();
        String selector = opts.getString(ReceiveOption.SELECTOR);

        try (JMSConsumer consumer = jmsContext.createConsumer(destination, selector)) {
            javax.jms.Message message;
            if (timeout == -1) {
                message = consumer.receiveNoWait();
            } else {
                message = consumer.receive(timeout);
            }

            if (message != null) {
                String contentType = JMSMessage.contentType(message);
                Codec codec = codecs.forContentType(contentType);
                return new JMSMessage(message, codec, this);
            } else {
                return null;
            }
        } finally {
            context.close();
        }
    }

    @Override
    public void stop() throws Exception {
        if (!this.stopped) {
            this.broker.destroyDestination(this);
            this.stopped = true;
        }
    }

    protected JMSMessaging broker() {
        return this.broker;
    }


    public static String jndiName(String name, String type) {
        return ("java:/jms/" + type + '/' + name).replace("//", "/_/");
    }

    public static boolean isJndiName(String name) {
        return name.startsWith("java:");
    }

    private final String name;
    private final Destination jmsDestination;
    private boolean stopped = false;
    private final JMSMessaging broker;
}
