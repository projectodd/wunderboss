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
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;
import org.projectodd.wunderboss.messaging.Session;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HornetQQueue extends HornetQDestination implements Queue {
    protected static final String SYNC_PROPERTY = "synchronous";
    protected static final String REQUEST_ID_PROPERTY = "sync_request_id";

    public HornetQQueue(javax.jms.Queue queue, HornetQMessaging broker) {
        super(queue, broker);
    }

    @Override
    public Listener respond(final MessageHandler handler,
                            Map<ListenOption, Object> options) throws Exception {
        final Options<ListenOption> opts = new Options<>(options);
        String selector = SYNC_PROPERTY + " = TRUE";
        if (opts.has(ListenOption.SELECTOR)) {
            selector += " AND " + opts.getString(ListenOption.SELECTOR);
        }
        opts.put(ListenOption.SELECTOR, selector);

        MessageHandler wrappedHandler = new MessageHandler() {
            @Override
            public Object onMessage(Message msg, Session session) throws Exception {
                Object result = handler.onMessage(msg, session);
                Options<MessageOpOption> replyOptions = new Options<>();
                replyOptions.put(SendOption.TTL, opts.getInt(RespondOption.TTL));
                replyOptions.put(SendOption.SESSION, session);
                if (result instanceof String) {
                    ((ReplyableMessage)msg).reply((String)result, msg.contentType(), replyOptions);
                } else {
                    ((ReplyableMessage)msg).reply((byte[])result, msg.contentType(), replyOptions);
                }
                return null;
            }
        };

        return listen(wrappedHandler, opts);
    }

    @Override
    public Response request(String content, String contentType,
                            Map<MessageOpOption, Object> options) throws Exception {
        return _request(content, contentType, options);
    }

    @Override
    public Response request(byte[] content, String contentType,
                            Map<MessageOpOption, Object> options) throws Exception {
        return _request(content, contentType, options);
    }

    public static String fullName(String name) {
        return "jms.queue." + name;
    }

    @Override
    public String jmsName() {
        return fullName(name());
    }

    @Override
    public String name() {
        try {
            return ((javax.jms.Queue)destination()).getQueueName();
        } catch (JMSException ffs) {
            ffs.printStackTrace();
            return null;
        }
    }

    private Response _request(Object message, String contentType,
                             Map<MessageOpOption, Object> options) throws Exception {
        Options<MessageOpOption> opts = new Options<>(options);
        final String id = UUID.randomUUID().toString();
        _send(message, contentType, options,
              new HashMap<String, Object>() {{
                  put(SYNC_PROPERTY, true);
                  put(REQUEST_ID_PROPERTY, id);
              }});

        return new HornetQResponse(id, this, connection(opts.get(MessageOpOption.CONNECTION)));
    }
}
