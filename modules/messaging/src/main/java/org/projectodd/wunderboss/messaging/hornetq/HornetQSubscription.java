package org.projectodd.wunderboss.messaging.hornetq;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Subscription;

import javax.jms.Session;
import javax.jms.Topic;

public class HornetQSubscription implements Subscription {

    public HornetQSubscription(HornetQMessaging broker,
                               Endpoint endpoint,
                               String name,
                               String selector) {
        this.broker = broker;
        this.endpoint = (HornetQEndpoint)endpoint;
        this.name = name;
        this.selector = selector;
    }

    @Override
    public Endpoint endpoint() {
        return this.endpoint;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String selector() {
        return this.selector;
    }

    @Override
    public void close() throws Exception {
        if (this.started) {
            Options<Messaging.CreateConnectionOption> options = new Options<>();
            options.put(Messaging.CreateConnectionOption.SUBSCRIPTION, this);
            try (
                    HornetQConnection connection = (HornetQConnection)this.broker.createConnection(options);
                    Session session = connection.jmsConnection().createSession();
            ) {
                session.unsubscribe(this.name);
            }
            this.started = false;
        }
    }

    public HornetQSubscription start() throws Exception {
        if (!this.started) {
            if (this.endpoint.destination() instanceof Topic) {
                Options<Messaging.CreateConnectionOption> options = new Options<>();
                options.put(Messaging.CreateConnectionOption.SUBSCRIPTION, this);
                try (
                        HornetQConnection connection = (HornetQConnection)this.broker.createConnection(options);
                        Session session = connection.jmsConnection().createSession();
                ) {
                    session.createDurableSubscriber((Topic)this.endpoint.destination(),
                                                    this.name, this.selector, false);
                }
            } else {
                log.warn("Subscription requested for a non-Topic destination, ignoring");
            }
            this.started = true;
        }

        return this;
    }

    private final HornetQMessaging broker;
    private final HornetQEndpoint endpoint;
    private final String name;
    private final String selector;
    private boolean started = false;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.messaging");
}
