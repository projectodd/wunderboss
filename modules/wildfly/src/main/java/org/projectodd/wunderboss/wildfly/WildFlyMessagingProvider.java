package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Messaging;

public class WildFlyMessagingProvider implements ComponentProvider<Messaging> {

    @Override
    public Messaging create(String name, Options options) {
        return new WildFlyMessaging(name, options);
    }
}
