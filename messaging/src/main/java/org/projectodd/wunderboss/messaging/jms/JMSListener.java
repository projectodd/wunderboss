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

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.WithCloseables;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

public class JMSListener extends WithCloseables implements Listener, MessageListener {

    protected final MessageConsumer consumer;

    public JMSListener(MessageHandler handler, Codecs codecs, Destination endpoint, JMSSpecificContext context, MessageConsumer consumer) {
        this.codecs = codecs;
        this.endpoint = endpoint;
        this.handler = handler;
        this.context = context;
        this.consumer = consumer;
        addCloseable(consumer);
        addCloseable(context);

    }

    public JMSListener start() {
        start(new Runnable() {
            @Override
            public void run() {
                try {
                    consumer.setMessageListener(JMSListener.this);
                } catch (Exception e) {
                    log.error("Failed to start handler: ", e);
                }
            }
        });

        return this;
    }

    public void start(Runnable r) {
        if (!this.started) {
            r.run();
        }

        this.started = true;
    }

    public void stop() {
        if (this.started) {
            try {
                closeCloseables();
            } catch (Exception e) {
                log.error("Failed to stop handler: ", e);
            }

            this.started = false;
        }
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public void onMessage(Message message) {
        try {
            JMSContext.currentContext.set(this.context.asNonCloseable());
            this.handler.onMessage(new JMSMessage(message,
                                                      this.codecs.forContentType(JMSMessage.contentType(message)),
                                                      this.endpoint),
                                   this.context);

            this.context.commit();
        } catch (Throwable e) {
            this.context.rollback();
            throw new RuntimeException("Unexpected error handling message", e);
        } finally {
            JMSContext.currentContext.remove();
        }

    }

    protected static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
    protected final MessageHandler handler;
    protected final Codecs codecs;
    protected final Destination endpoint;
    protected final JMSSpecificContext context;
    protected boolean started = false;
}
