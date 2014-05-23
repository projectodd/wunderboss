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
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Destination.MessageOpOption;
import org.projectodd.wunderboss.messaging.Destination.SendOption;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;

import javax.jms.JMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HornetQMessage implements ReplyableMessage {

    public HornetQMessage(javax.jms.Message message, Destination destination, HornetQConnection connection) {
        this.baseMessage = message;
        this.connection = connection;
        this.destination = destination;
    }

    @Override
    public String contentType() {
        try {
            return this.baseMessage.getStringProperty(HornetQDestination.CONTENT_TYPE_PROPERTY);
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
    public <T> T body(Class T) {
        try {
            return (T)this.baseMessage.getBody(T);
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
    public Response reply(String content, String contentType,
                          Map<MessageOpOption, Object> options) throws Exception {
        this.destination.send(content, contentType, replyOptions(options));

        return new HornetQResponse(requestID(), this.destination, this.connection);
    }

    @Override
    public Response reply(byte[] content, String contentType,
                          Map<MessageOpOption, Object> options) throws Exception {
        this.destination.send(content, contentType, replyOptions(options));

        return new HornetQResponse(requestID(), this.destination, this.connection);
    }

    protected String requestID() {
        try {

            return this.baseMessage.getStringProperty(HornetQQueue.REQUEST_ID_PROPERTY);
        } catch (JMSException ffs) {
            ffs.printStackTrace();

            return null;
        }
    }
    protected Options<MessageOpOption> replyOptions(Map<Destination.MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        Map<String, Object> properties = (Map<String, Object>)opts.get(Destination.SendOption.PROPERTIES);
        if (properties == null) {
            properties = new HashMap<>();
            opts.put(SendOption.PROPERTIES, properties);
        }
        //gross, we're modifying the original properties map
        properties.put("JMSCorrelationID", requestID());

        return opts;
    }

    public javax.jms.Message jmsMessage() {
        return this.baseMessage;
    }

    private final javax.jms.Message baseMessage;
    private final HornetQConnection connection;
    private final Destination destination;
}
