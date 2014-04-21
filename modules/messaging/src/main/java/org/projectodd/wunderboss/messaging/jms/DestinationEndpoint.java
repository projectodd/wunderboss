package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.messaging.Endpoint;

import javax.jms.Destination;
import javax.jms.Topic;

public abstract class DestinationEndpoint implements Endpoint<Destination> {

    public DestinationEndpoint(Destination dest, boolean durable) {
        this.destination = dest;
        this.durable = durable;
    }

    @Override
    public boolean isBroadcast() {
        return (this.destination instanceof Topic);
    }

    @Override
    public boolean isDurable() {
        return this.durable;
    }

    @Override
    public Destination implementation() {
        return this.destination;
    }

    private final Destination destination;
    private final boolean durable;
}
