package org.projectodd.wunderboss.messaging.hornetq;

import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.embedded.EmbeddedJMS;

public class EmbeddedServer extends EmbeddedJMS {
    public JMSServerManager serverManager() {
        return this.serverManager;
    }
}
