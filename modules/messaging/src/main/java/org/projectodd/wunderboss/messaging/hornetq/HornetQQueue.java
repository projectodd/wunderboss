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
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Reply;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;
import org.projectodd.wunderboss.messaging.Session;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HornetQQueue extends HornetQDestination implements Queue {
    protected static final String SYNC_PROPERTY = "synchronous";
    protected static final String SYNC_RESPONSE_PROPERTY = "synchronous_response";
    protected static final String REQUEST_ID_PROPERTY = "sync_request_id";
    protected static final String REQUEST_NODE_ID_PROPERTY = "sync_request_node_id";

    public HornetQQueue(javax.jms.Queue queue, HornetQMessaging broker) {
        super(queue, broker);
    }

    @Override
    public Listener respond(final MessageHandler handler,
                            final Codecs codecs,
                            Map<ListenOption, Object> options) throws Exception {
        final Options<ListenOption> opts = new Options<>(options);
        String selector = SYNC_PROPERTY + " = TRUE";
        if (opts.has(ListenOption.SELECTOR)) {
            selector += " AND " + opts.getString(ListenOption.SELECTOR);
        }
        opts.put(ListenOption.SELECTOR, selector);

        MessageHandler wrappedHandler = new MessageHandler() {
            @Override
            public Reply onMessage(Message msg, Session session) throws Exception {
                Reply result = handler.onMessage(msg, session);
                Options<MessageOpOption> replyOptions = new Options<>();
                replyOptions.put(SendOption.TTL, opts.getInt(RespondOption.TTL));
                replyOptions.put(SendOption.SESSION, session);
                replyOptions.put(SendOption.PROPERTIES, result.properties());
                ((ReplyableMessage)msg).reply(result.content(), codecs.forContentType(msg.contentType()), replyOptions);

                return null;
            }
        };

        return listen(wrappedHandler, codecs, opts);
    }

    @Override
    public Response request(Object content, Codec codec,
                            Codecs codecs,
                            Map<MessageOpOption, Object> options) throws Exception {
        final Options<MessageOpOption> opts = new Options<>(options);
        final String id = UUID.randomUUID().toString();
        //TODO: there's probably a better way to get this
        final String nodeId = System.getProperty("jboss.node.name", "node1");
        final HornetQResponse response = new HornetQResponse();
        Options<ListenOption> routerOpts = new Options<>();
        routerOpts.put(ListenOption.SELECTOR,
                       REQUEST_NODE_ID_PROPERTY + " = '" + nodeId + "' AND " +
                               SYNC_RESPONSE_PROPERTY + " = TRUE");
        if (opts.has(MessageOpOption.CONNECTION)) {
            routerOpts.put(ListenOption.CONNECTION, opts.get(MessageOpOption.CONNECTION));
        }

        ResponseRouter.routerFor(this, codecs, routerOpts).registerResponse(id, response);

        _send(content, codec, options,
              new HashMap<String, Object>() {{
                  put(REQUEST_NODE_ID_PROPERTY, nodeId);
                  put(SYNC_PROPERTY, true);
                  put(REQUEST_ID_PROPERTY, id);
              }});

        return response;
    }

    public static String fullName(String name) {
        return "jms.queue." + name;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        ResponseRouter.closeRouterFor(this);
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

}
