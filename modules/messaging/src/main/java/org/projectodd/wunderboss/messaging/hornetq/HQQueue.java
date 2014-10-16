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
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Reply;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;

import javax.jms.Destination;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HQQueue extends HQDestination implements Queue {

    public HQQueue(String name, Destination destination, HQMessaging broker) {
        super(name, destination, broker);
    }

    @Override
    public Listener respond(final MessageHandler handler,
                            final Codecs codecs,
                            Map<ListenOption, Object> options) throws Exception {
        final Options<ListenOption> opts = new Options<>(options);
        String selector = HQMessage.SYNC_PROPERTY + " = TRUE";
        if (opts.has(ListenOption.SELECTOR)) {
            selector += " AND " + opts.getString(ListenOption.SELECTOR);
        }
        opts.put(ListenOption.SELECTOR, selector);

        MessageHandler wrappedHandler = new MessageHandler() {
            @Override
            public Reply onMessage(Message msg, Context context) throws Exception {
                Reply result = handler.onMessage(msg, context);
                Options<MessageOpOption> replyOptions = new Options<>();
                replyOptions.put(PublishOption.TTL, opts.getInt(RespondOption.TTL));
                replyOptions.put(PublishOption.CONTEXT, context);
                replyOptions.put(PublishOption.PROPERTIES, result.properties());
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
        final HQResponse response = new HQResponse();
        Options<ListenOption> routerOpts = new Options<>();
        routerOpts.put(ListenOption.SELECTOR,
                       HQMessage.REQUEST_NODE_ID_PROPERTY + " = '" + nodeId + "' AND " +
                               HQMessage.SYNC_RESPONSE_PROPERTY + " = TRUE");
        if (opts.has(MessageOpOption.CONTEXT)) {
            routerOpts.put(ListenOption.CONTEXT, opts.get(MessageOpOption.CONTEXT));
        }

        ResponseRouter.routerFor(this, codecs, routerOpts).registerResponse(id, response);

        publish(content, codec, options,
                new HashMap<String, Object>() {{
                    put(HQMessage.REQUEST_NODE_ID_PROPERTY, nodeId);
                    put(HQMessage.SYNC_PROPERTY, true);
                    put(HQMessage.REQUEST_ID_PROPERTY, id);
                }});

        return response;
    }

    public static String jmsName(String name) {
        return "jms.queue." + name;
    }

    public static String fullName(String name) {
        if (isJndiName(name)) {
            return name;
        } else {
            return jmsName(name);
        }
    }

    @Override
    public String jmsName() {
        return jmsName(name());
    }

    @Override
    public String fullName() {
        return fullName(name());
    }

}
