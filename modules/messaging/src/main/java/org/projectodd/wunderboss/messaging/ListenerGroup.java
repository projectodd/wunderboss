package org.projectodd.wunderboss.messaging;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Messaging.CreateConnectionOption;
import org.projectodd.wunderboss.messaging.Messaging.ListenOption;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XAConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListenerGroup {

    public ListenerGroup(Messaging broker,
                         MessageListener listener,
                         Destination destination,
                         Options<ListenOption> options) {
        this.broker = broker;
        this.listener = listener;
        this.destination = destination;
        this.options = options;
    }

    public synchronized ListenerGroup start() {
        if (!this.started) {
            try {
                startConnection();

                int concurrency = this.options.getInt(ListenOption.CONCURRENCY, 1);
                while(concurrency-- > 0) {
                    Session session = createSession();
                    listeners.add(new TransactionalListener(this.listener, session,
                                                            createConsumer(session),
                                                            isXAEnabled(),
                                                            //TODO: a transaction manager
                                                            null).start());
                }

            } catch (Exception e) {
                log.error("Failed to start listener group: ", e);
            }

            this.started = true;
        }

        return this;
    }

    public synchronized void stop() {
        if (this.started) {
            try {
                this.connection.close();

                for(TransactionalListener each : this.listeners) {
                    each.stop();
                }

                this.listeners.clear();
            } catch (Exception e) {
                log.error("Failed to stop listener group: ", e);
            }

            this.started = false;
        }
    }

    protected void startConnection() throws Exception {

        this.connection =
                broker.createConnection(new HashMap<CreateConnectionOption, Object>() {{
                    put(Messaging.CreateConnectionOption.XA, isXAEnabled());
                }});

        String clientID = this.options.getString(ListenOption.CLIENT_ID);
        if (isDurable()) {
            if (clientID != null) {
                if (this.destination instanceof Topic) {
                    log.info("Setting clientID to " + clientID);
                    this.connection.setClientID(clientID);
                } else {
                    log.warn("ClientID set for listener but " +
                                     destination + " is not a topic - ignoring.");
                }
            } else {
                throw new IllegalArgumentException("Durable topic listeners require a client_id.");
            }
        }

        this.connection.start();
    }

    protected Session createSession() throws JMSException {
        if (isXAEnabled()) {
            return ((XAConnection)this.connection).createXASession();
        } else {
            // Use local transactions for non-XA message processors
            return this.connection.createSession(true, Session.SESSION_TRANSACTED);
        }
    }

    protected MessageConsumer createConsumer(Session session) throws JMSException {
        String selector = this.options.getString(ListenOption.SELECTOR);
        String name = this.options.getString(ListenOption.SUBSCRIBER_NAME,
                                             this.options.getString(ListenOption.LISTENER_ID,
                                                                    this.options.getString(ListenOption.CLIENT_ID)));
        if (isDurable() && this.destination instanceof Topic) {
            return session.createDurableSubscriber((Topic) destination,
                                                   name, selector, false);
        } else {
            return session.createConsumer(destination, selector);
        }

    }
    protected boolean isXAEnabled() {
        return this.options.getBoolean(ListenOption.XA, this.broker.isXaDefault());
    }

    protected boolean isDurable() {
        return this.options.getBoolean(ListenOption.DURABLE, false);
    }

    private final Messaging broker;
    private final MessageListener listener;
    private final Destination destination;
    private final Options<ListenOption> options;
    private Connection connection;
    private final List<TransactionalListener> listeners = new ArrayList<>();
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");

}
