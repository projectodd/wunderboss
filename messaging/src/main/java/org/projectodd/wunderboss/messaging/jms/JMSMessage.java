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
import org.projectodd.wunderboss.codecs.None;
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Destination.MessageOpOption;
import org.projectodd.wunderboss.messaging.ReplyableMessage;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMSMessage implements ReplyableMessage {

    public static final String CONTENT_TYPE_PROPERTY = "contentType";
    public static final String SYNC_PROPERTY = "synchronous";
    public static final String SYNC_RESPONSE_PROPERTY = "synchronous_response";
    public static final String REQUEST_ID_PROPERTY = "sync_request_id";
    public static final String REQUEST_NODE_ID_PROPERTY = "sync_request_node_id";

    public JMSMessage(javax.jms.Message message, Codec codec,
               Destination destination) {
        this.baseMessage = message;
        this.codec = (codec == null ? None.INSTANCE : codec);
        this.destination = destination;
    }

    @Override
    public String id() {
        try {
            return this.baseMessage.getJMSMessageID();
        } catch (JMSException e) {
            throw new IllegalStateException("Failed to read id from message", e);
        }
    }

    @Override
    public String contentType() {
        return contentType(this.baseMessage);
    }

    public static String contentType(javax.jms.Message message) {
        try {
            return message.getStringProperty(CONTENT_TYPE_PROPERTY);
        } catch (JMSException e) {
            throw new IllegalStateException("Failed to read property from message", e);
        }
    }

    @Override
    public Map<String, Object> properties() {
        Map<String, Object> headers = new HashMap<>();
        try {
            for(String name : (List<String>)Collections.list(this.baseMessage.getPropertyNames())) {
             headers.put(name, this.baseMessage.getObjectProperty(name));
            }
        } catch (JMSException e) {
            throw new IllegalStateException("Failed to read properties from message", e);
        }

        return headers;
    }

    @Override
    public Destination endpoint() {
        return this.destination;
    }

    @Override
    public Object body() {
        Object body;
        try {
            if (this.baseMessage instanceof TextMessage) {
                body = ((TextMessage)this.baseMessage).getText();
            } else if (this.baseMessage instanceof ObjectMessage) {
                body = ((ObjectMessage)this.baseMessage).getObject();
            } else if (this.baseMessage instanceof BytesMessage) {
                body = new byte[(int)((BytesMessage) this.baseMessage).getBodyLength()];
                ((BytesMessage)this.baseMessage).readBytes((byte[]) body);
            } else {
                throw new IllegalArgumentException("Don't know how to deal with message of type "
                                                           + this.baseMessage.getClass());
            }

            return this.codec.decode(body);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean acknowledge() throws Exception {
        this.baseMessage.acknowledge();

        return true;
    }

    @Override
    public void reply(Object content, Codec codec,
                      Map<MessageOpOption, Object> options) throws Exception {
        this.destination.publish(content, codec, replyOptions(options));
    }

    @Override
    public String requestID() {
        try {

            return this.baseMessage.getStringProperty(REQUEST_ID_PROPERTY);
        } catch (JMSException ffs) {
            ffs.printStackTrace();

            return null;
        }
    }

    @Override
    public String requestNodeID() {
        try {

            return this.baseMessage.getStringProperty(REQUEST_NODE_ID_PROPERTY);
        } catch (JMSException ffs) {
            ffs.printStackTrace();

            return null;
        }
    }

    protected Options<MessageOpOption> replyOptions(Map<Destination.MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        Map<String, Object> properties = (Map<String, Object>)opts.get(Destination.PublishOption.PROPERTIES);
        Map<String, Object> newProperties = new HashMap<>();
        if (properties != null) {
            newProperties.putAll(properties);
        }
        newProperties.put(SYNC_RESPONSE_PROPERTY, true);
        newProperties.put(REQUEST_ID_PROPERTY, requestID());
        newProperties.put(REQUEST_NODE_ID_PROPERTY, requestNodeID());

        opts.put(Destination.PublishOption.PROPERTIES, newProperties);

        return opts;
    }

    public javax.jms.Message jmsMessage() {
        return this.baseMessage;
    }

    private final javax.jms.Message baseMessage;
    private final Codec codec;
    private final Destination destination;
}
