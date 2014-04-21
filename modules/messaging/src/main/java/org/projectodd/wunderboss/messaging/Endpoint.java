package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Closeable;
import org.projectodd.wunderboss.Implementation;

public interface Endpoint<T> extends Implementation<T>, Closeable {

    /**
     * Indicates if this is a broadcast endpoint (a topic) or not
     */
    boolean isBroadcast();

    boolean isDurable();
}
