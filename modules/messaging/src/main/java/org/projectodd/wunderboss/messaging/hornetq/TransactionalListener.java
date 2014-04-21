package org.projectodd.wunderboss.messaging.hornetq;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.jms.client.HornetQMessage;
import org.hornetq.jms.client.HornetQMessageConsumer;
import org.hornetq.jms.client.HornetQSession;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.MessageHandler;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.XASession;
import javax.transaction.TransactionManager;
import java.lang.reflect.Field;

public class TransactionalListener implements MessageListener, org.hornetq.api.core.client.MessageHandler {
    public TransactionalListener(MessageHandler handler,
                                 Endpoint endpoint,
                                 Connection connection,
                                 Session session,
                                 MessageConsumer consumer, boolean xa, TransactionManager tm) {
        this.handler = handler;
        this.endpoint = endpoint;
        this.connection = connection;
        this.session = session;
        this.consumer = consumer;
        this.xa = xa;
        this.tm = tm;
    }

    public TransactionalListener start() {
        if (!this.started) {
            try {
                // Use HornetQ's Core API for message consumers where possible so we
                // get proper XA support. Otherwise, fall back to standard JMS.
                if (consumer instanceof HornetQMessageConsumer) {
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
                } else {
                    consumer.setMessageListener( this );
                }
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
                //TODO: do we need to close the hornetQSession and clientConsumer?
            } catch (Exception e) {
                log.error("Failed to stop handler: ", e);
            }

            this.started = false;
        }
    }

    /**
     * This entire method is essentially a copy of HornetQ's
     * JMSMessageListenerWrapper onMessage but with hooks for preparing
     * transactions before calling MessageListener's onMessage
     *
     */
    @Override
    public void onMessage(final ClientMessage message) {
        ClientSession coreSession = this.hornetQSession.getCoreSession();
        HornetQMessage msg = HornetQMessage.createMessage(message, coreSession);
        log.trace("MessageHandler.onMessage called with messageId " + msg.getJMSMessageID());

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
            onMessage(msg);
        } catch (RuntimeException e) {
            log.warn("Unhandled exception thrown from onMessage", e);

            if (!transactedOrClientAck) {
                try {
                    log.trace("Rolling back messageId " + msg.getJMSMessageID());
                    coreSession.rollback(true);
                    this.hornetQSession.setRecoverCalled(true);
                } catch (Exception e2) {
                    log.error("Failed to recover session", e2);
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

    @Override
    public void onMessage(Message message) {
        try {
            try {
                this.handler.onMessage(new org.projectodd.wunderboss.messaging.hornetq.HornetQMessage(message,
                                                                                                      this.endpoint,
                                                                                                      this.connection));
                if (this.xa) {
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
        }
    }

    private void prepareTransaction() {
        try {
            this.tm.begin();
            this.tm.getTransaction().enlistResource(((XASession)this.session).getXAResource());
        } catch (Throwable e) {
            log.error("Failed to prepare transaction for message", e);
        }
    }

    private final MessageHandler handler;
    private final Endpoint endpoint;
    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;
    private final boolean xa;
    private final TransactionManager tm;
    private HornetQSession hornetQSession;
    private ClientConsumer clientConsumer;
    private boolean transactedOrClientAck;
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
