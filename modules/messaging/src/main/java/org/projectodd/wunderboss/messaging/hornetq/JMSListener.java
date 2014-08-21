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
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;

public class JMSListener implements Listener, MessageListener { //, org.hornetq.api.core.client.MessageHandler {
    public JMSListener(MessageHandler handler,
                       Codecs codecs,
                       Destination endpoint,
                       HornetQConnection connection,
                       HornetQSession session,
                       JMSConsumer consumer) {
        this.handler = handler;
        this.codecs = codecs;
        this.endpoint = endpoint;
        this.connection = connection;
        this.session = session;
        this.consumer = consumer;
    }

    public JMSListener start() {
        if (!this.started) {
            try {
                // Use HornetQ's Core API for message consumers where possible so we
                // get proper XA support. Otherwise, fall back to standard JMS.
                /*if (consumer instanceof HornetQMessageConsumer) {
                    log.trace("Using HornetQ Core API for handler");
                    Field sessionField = consumer.getClass().getDeclaredField("session");
                    sessionField.setAccessible(true);
                    this.hornetQSession = (HornetQSession) sessionField.get( consumer );

                    Field consumerField = consumer.getClass().getDeclaredField( "consumer" );
                    consumerField.setAccessible( true );
                    this.clientConsumer = (ClientConsumer) consumerField.get( consumer );

                    int ackMode = hornetQSession.getAcknowledgeMode();
                    this.transactedOrClientAck = (ackMode == Session.SESSION_TRANSACTED || ackMode == Session.CLIENT_ACKNOWLEDGE) || hornetQSession.isXA();

                    this.clientConsumer.setMessageHandler( this );
                } else {*/
                consumer.setMessageListener(this);
                //}
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
                this.session.close();
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

    /**
     * This entire method is essentially a copy of HornetQ's
     * JMSMessageListenerWrapper onMessage but with hooks for preparing
     * transactions before calling MessageListener's onMessage
     *
     */
    /*
    @Override
    public void onMessage(final ClientMessage message) {
        ClientSession coreSession = this.hornetQSession.getCoreSession();
        HornetQMessage msg = HornetQMessage.createMessage(message, coreSession);
        log.trace("MessageHandler.onMessage called with messageId " + message.getMessageID());

        try {
            msg.doBeforeReceive();
        } catch (Exception e) {
            log.error("Failed to prepare message for receipt", e);
            return;
        }

        if (this.xa) {
            log.trace("Preparing transaction for messageId " + msg.getJMSMessageID());
            prepareTransaction();
        }

        if (transactedOrClientAck) {
            try {
                message.acknowledge();
                log.trace("Acknowledging messageId " + msg.getJMSMessageID() + " before calling onMessage");
            } catch (HornetQException e) {
                log.error("Failed to process message", e);
            }
        }

        try {
            onMessage(message);
        } catch (RuntimeException e) {
            log.warn("Unhandled exception thrown from onMessage", e);
            if (!transactedOrClientAck) {
                try {
                    log.trace("Rolling back messageId " + msg.getJMSMessageID());
                    coreSession.rollback(true);
                    this.hornetQSession.setRecoverCalled(true);
                } catch (Exception e2) {
                    log.error("Failed to recover context", e2);
                }
            }
        }

        if (!this.hornetQSession.isRecoverCalled()) {
            try {
                // We don't want to call this if the consumer was closed from inside onMessage
                if (!clientConsumer.isClosed() && !transactedOrClientAck) {
                    log.trace("Acknowledging messageId " + msg.getJMSMessageID() + " after calling onMessage");
                    message.acknowledge();
                }
            } catch (Exception e) {
                log.error("Failed to process message", e);
            }
        }

       this.hornetQSession.setRecoverCalled(false);
    }
       */

    @Override
    public void onMessage(Message message) {
        final JMSContext context = this.session.context();
        try {
            HornetQSession.currentSession.set(this.session);
            this.handler.onMessage(new HornetQMessage(message,
                                                      this.codecs.forContentType(HornetQMessage.contentType(message)),
                                                      this.endpoint,
                                                      this.connection),
                                   this.session);

            if (context.getTransacted()) {
                context.commit();
            }
        } catch (Exception e) {
            log.warn("Unhandled exception thrown from onMessage", e);
            if (context.getTransacted()) {
                context.rollback();
            }
        } finally {
            HornetQSession.currentSession.remove();
        }

         /*       if (this.xa) {
                    this.tm.commit();
                }
            } catch (javax.transaction.RollbackException ignored) {
            } catch (Throwable e) {
                e.printStackTrace();
                if (this.xa) {
                    this.tm.rollback();
                }
                throw(e);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected error processing message", e);
        }*/
    }

   /* private void prepareTransaction() {
        try {
            this.tm.begin();
            this.tm.getTransaction().enlistResource(((XASession)this.context).getXAResource());
        } catch (Throwable e) {
            log.error("Failed to prepare transaction for message", e);
        }
    }*/

    private final MessageHandler handler;
    private final Codecs codecs;
    private final Destination endpoint;
    private final HornetQConnection connection;
    private final HornetQSession session;
    private final JMSConsumer consumer;
    //private final boolean xa;
    //private final TransactionManager tm;
   /* private HornetQSession hornetQSession;
    private ClientConsumer clientConsumer;
    private boolean transactedOrClientAck;*/
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
