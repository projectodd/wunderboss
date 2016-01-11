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

package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Reply;
import org.projectodd.wunderboss.messaging.WithCloseables;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class JMSDestination extends WithCloseables implements Destination {

    protected static void fillInProperties(javax.jms.Message message, Map<String, Object> properties) throws JMSException {
        for(Map.Entry<String, Object> each : properties.entrySet()) {
            message.setObjectProperty(each.getKey(), each.getValue());
        }
    }

    @Override
    public int defaultConcurrency() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public Listener listen(final MessageHandler handler, Codecs codecs, Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        Context givenContext = (Context)opts.get(ListenOption.CONTEXT);

        if (givenContext != null &&
                !givenContext.isRemote()) {
            throw new IllegalArgumentException("Listening only accepts a remote context.");
        }

        MessageHandler wrappedHandler = new MessageHandler() {
            @Override
            public Reply onMessage(Message msg, Context ctx) throws Exception {
                ((JMSSpecificContext)ctx).setLatestMessage((JMSMessage)msg);

                return handler.onMessage(msg, ctx);
            }
        };

        final JMSSpecificContext context = context(givenContext);

        Listener listener = new JMSMessageHandlerGroup(context,
                                                       wrappedHandler,
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

    protected JMSSpecificContext context(final Object context) throws Exception {
        Context newContext;
        JMSSpecificContext threadContext = JMSContext.currentContext.get();

        if (context != null) {
            newContext = ((JMSSpecificContext) context).asNonCloseable();
        } else if (TransactionUtil.isTransactionActive()) {
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
        Session session = context.jmsSession();
        javax.jms.Destination destination = jmsDestination();
        MessageProducer producer = session.createProducer(destination);

        try {
            Object encoded = codec.encode(message);
            Class encodesTo = codec.encodesTo();
            javax.jms.Message jmsMessage;

            if (encodesTo == String.class) {
                jmsMessage = session.createTextMessage((String)encoded);
            } else if (encodesTo == byte[].class) {
                jmsMessage = session.createBytesMessage();
                ((BytesMessage)jmsMessage).writeBytes((byte[]) encoded);
            } else {
                jmsMessage = session.createObjectMessage((Serializable)encoded);
            }

            fillInProperties(jmsMessage, (Map<String, Object>) opts.get(PublishOption.PROPERTIES, Collections.emptyMap()));
            fillInProperties(jmsMessage, additionalProperties);
            jmsMessage.setStringProperty(JMSMessage.CONTENT_TYPE_PROPERTY, codec.contentType());

            producer.send(jmsMessage,
                          opts.getBoolean(PublishOption.PERSISTENT) ?
                                  DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT,
                          opts.getInt(PublishOption.PRIORITY),
                          opts.getLong(PublishOption.TTL, producer.getTimeToLive()));
        } finally {
            producer.close();
            context.close();
        }
    }

    @Override
    public Message receive(Codecs codecs, Map<MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        int timeout = opts.getInt(ReceiveOption.TIMEOUT);
        JMSSpecificContext context = context(opts.get(MessageOpOption.CONTEXT));
        Session jmsSession = context.jmsSession();
        javax.jms.Destination destination = jmsDestination();
        String selector = opts.getString(ReceiveOption.SELECTOR);
        MessageConsumer consumer = jmsSession.createConsumer(destination, selector);

        try {
            javax.jms.Message message;
            if (timeout == -1) {
                message = consumer.receiveNoWait();
            } else {
                message = consumer.receive(timeout);
            }

            if (message != null) {
                String contentType = JMSMessage.contentType(message);
                Codec codec = codecs.forContentType(contentType);

                JMSMessage wrappedMessage = new JMSMessage(message, codec, this);
                // so we can acknowledge from the context
                context.setLatestMessage(wrappedMessage);

                return wrappedMessage;
            } else {

                return null;
            }
        } finally {
            consumer.close();
            context.close();
        }
    }

    public enum Type {
        QUEUE("queue"), TOPIC("topic");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }

    public JMSDestination(String name, javax.jms.Destination destination, JMSMessagingSkeleton broker) {
        this.jmsDestination = destination;
        this.name = name;
        this.broker = broker;
    }

    @Override
    public String name() {
        return this.name;
    }

    public javax.jms.Destination jmsDestination() {
        return this.jmsDestination;
    }

    public abstract Type type();

    public String jmsName() {
        return DestinationUtil.jmsName(name(), type());
    }

    public String fullName() {
        return DestinationUtil.fullName(name(), type());
    }

    @Override
    public void stop() throws Exception {
        if (!this.stopped) {
            closeCloseables();
            this.broker.destroyDestination(this);
            this.stopped = true;
        }
    }

    protected JMSMessagingSkeleton broker() {
        return this.broker;
    }

    protected final String name;
    protected final javax.jms.Destination jmsDestination;
    protected final JMSMessagingSkeleton broker;
    private boolean stopped = false;
}
