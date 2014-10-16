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

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.codecs.Codecs;
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.MessageHandler;

import javax.jms.JMSConsumer;
import javax.jms.Message;
import javax.jms.MessageListener;

public class JMSListener implements Listener, MessageListener {
    public JMSListener(MessageHandler handler,
                       Codecs codecs,
                       Destination endpoint,
                       HQContext context,
                       JMSConsumer consumer) {
        this.handler = handler;
        this.codecs = codecs;
        this.endpoint = endpoint;
        this.context = context;
        this.consumer = consumer;
    }

    public JMSListener start() {
        if (!this.started) {
            try {
                consumer.setMessageListener(this);
            } catch (Exception e) {
                log.error("Failed to start handler: ", e);
            }

            this.started = true;
        }

        return this;
    }

    public void stop() {
        if (this.started) {
            try {
                this.context.close();
                this.consumer.close();
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
            ConcreteHQContext.currentContext.set(this.context.asNonCloseable());
            this.handler.onMessage(new HQMessage(message,
                                                      this.codecs.forContentType(HQMessage.contentType(message)),
                                                      this.endpoint),
                                   this.context);

            this.context.commit();
        } catch (Throwable e) {
            this.context.rollback();
            throw new RuntimeException("Unexpected error handling message", e);
        } finally {
            ConcreteHQContext.currentContext.remove();
        }

    }

    private final MessageHandler handler;
    private final Codecs codecs;
    private final Destination endpoint;
    private final HQContext context;
    private final JMSConsumer consumer;
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
