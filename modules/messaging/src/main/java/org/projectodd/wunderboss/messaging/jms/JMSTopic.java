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
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Topic;

import javax.jms.Destination;
import javax.jms.TopicSubscriber;
import java.util.HashMap;
import java.util.Map;

public class JMSTopic extends JMSDestination implements Topic {

    public JMSTopic(String name, Destination destination, JMSMessagingSkeleton broker) {
        super(name, destination, broker);
    }

    @Override
    public Listener subscribe(final String id, final MessageHandler handler,
                              final Codecs codecs,
                              final Map<SubscribeOption, Object> options) throws Exception {
        Options<SubscribeOption> opts = new Options<>(options);
        final JMSSpecificContext context = (JMSSpecificContext)context(id, opts.get(SubscribeOption.CONTEXT));
        final TopicSubscriber subscriber = context
                .jmsSession()
                .createDurableSubscriber((javax.jms.Topic) jmsDestination(),
                                         id,
                                         opts.getString(SubscribeOption.SELECTOR), false);

        final Listener listener = new JMSListener(handler,
                                                  codecs,
                                                  this,
                                                  context,
                                                  subscriber).start();

        Context parent = (Context)opts.get(SubscribeOption.CONTEXT);
        if (parent != null) {
            parent.addCloseable(listener);
        }

        broker().addCloseableForDestination(this, listener);

        return new Listener() {
            @Override
            public void close() throws Exception {
                listener.close();
                context.close();
            }
        };
    }

    @Override
    public void unsubscribe(String id, Map<UnsubscribeOption, Object> options) throws Exception {
        final Options<UnsubscribeOption> opts = new Options<>(options);
        try (JMSSpecificContext context = (JMSSpecificContext)context(id, opts.get(UnsubscribeOption.CONTEXT))) {
            context.jmsSession().unsubscribe(id);
        }
    }

    @Override
    public JMSDestination.Type type() {
        return JMSDestination.Type.TOPIC;
    }

    @Override
    public int defaultConcurrency() {
        return 1;
    }

    protected JMSSpecificContext context(final String id, final Object context) throws Exception {
        if (context != null) {
            return ((JMSSpecificContext)context).asNonCloseable();
        } else {
            return (JMSSpecificContext)broker()
                    .createContext(new HashMap<Messaging.CreateContextOption, Object>() {{
                        put(Messaging.CreateContextOption.CLIENT_ID, id);
                    }});
        }
    }

}
