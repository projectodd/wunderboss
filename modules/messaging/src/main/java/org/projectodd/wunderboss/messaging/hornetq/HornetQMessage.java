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
import org.projectodd.wunderboss.messaging.Connection.SendOption;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;

import javax.jms.JMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HornetQMessage implements ReplyableMessage {

    public HornetQMessage(javax.jms.Message message, Endpoint endpoint, Connection connection) {
        this.baseMessage = message;
        this.connection = connection;
        this.endpoint = endpoint;
    }

    @Override
    public String contentType() {
        try {
            return this.baseMessage.getStringProperty(HornetQConnection.CONTENT_TYPE_PROPERTY);
        } catch (JMSException e) {
            throw new IllegalStateException("Failed to read property from message", e);
        }
    }

    @Override
    public Map<String, Object> headers() {
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
    public Endpoint endpoint() {
        return this.endpoint;
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
                          Map<SendOption, Object> options) throws Exception {
        this.connection.send(this.endpoint, content, contentType, replyOptions(options));

        return new HornetQResponse(this.connection, this.baseMessage, this.endpoint);
    }

    @Override
    public Response reply(byte[] content, String contentType,
                          Map<SendOption, Object> options) throws Exception {
        this.connection.send(this.endpoint, content, contentType, replyOptions(options));

        return new HornetQResponse(this.connection, this.baseMessage, this.endpoint);
    }

    protected Options<SendOption> replyOptions(Map<SendOption, Object> options) throws Exception {
        Options<SendOption> opts = new Options<>(options);
        Map<String, Object> headers = (Map<String, Object>)opts.get(SendOption.HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            opts.put(SendOption.HEADERS, headers);
        }
        //gross, we're modifying the original headers map
        headers.put("JMSCorrelationID", this.baseMessage.getJMSMessageID());

        //ARGH! we need a selector on the listener. FFS
        return opts;
    }

    public javax.jms.Message jmsMessage() {
        return this.baseMessage;
    }

    private final javax.jms.Message baseMessage;
    private final Connection connection;
    private final Endpoint endpoint;
}
