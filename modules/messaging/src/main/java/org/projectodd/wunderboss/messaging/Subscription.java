package org.projectodd.wunderboss.messaging;

public interface Subscription extends AutoCloseable {
    Endpoint endpoint();

    String name();

    String selector();
}
