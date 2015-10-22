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

package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.ConcreteResponse;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Reply;
import org.projectodd.wunderboss.messaging.ReplyableMessage;
import org.projectodd.wunderboss.messaging.Response;
import org.projectodd.wunderboss.messaging.ResponseRouter;

import javax.jms.Destination;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JMSQueue extends JMSDestination implements Queue {

    public JMSQueue(String name, Destination destination, JMSMessagingSkeleton broker) {
        super(name, destination, broker);
    }

    @Override
    public Listener respond(final MessageHandler handler,
                            final Codecs codecs,
                            Map<ListenOption, Object> options) throws Exception {
        final Options<ListenOption> opts = new Options<>(options);
        String selector = JMSMessage.SYNC_PROPERTY + " = TRUE";
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
        final JMSSpecificContext context = (JMSSpecificContext)opts.get(MessageOpOption.CONTEXT);
        final String nodeId = context != null ? context.id() : JMSMessagingSkeleton.BROKER_ID;
        final ConcreteResponse response = new ConcreteResponse();
        Options<ListenOption> routerOpts = new Options<>();
        routerOpts.put(ListenOption.SELECTOR,
                       JMSMessage.REQUEST_NODE_ID_PROPERTY + " = '" + nodeId + "' AND " +
                               JMSMessage.SYNC_RESPONSE_PROPERTY + " = TRUE");
        if (context != null) {
            routerOpts.put(ListenOption.CONTEXT, context);
        }

        ResponseRouter.routerFor(this, codecs, routerOpts).registerResponse(id, response);

        publish(content, codec, options,
                new HashMap<String, Object>() {{
                    put(JMSMessage.REQUEST_NODE_ID_PROPERTY, nodeId);
                    put(JMSMessage.SYNC_PROPERTY, true);
                    put(JMSMessage.REQUEST_ID_PROPERTY, id);
                }});

        return response;
    }

    @Override
    public JMSDestination.Type type() {
        return JMSDestination.Type.QUEUE;
    }

}
