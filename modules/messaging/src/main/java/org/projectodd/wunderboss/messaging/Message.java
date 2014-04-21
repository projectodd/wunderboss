package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Implementation;

import java.util.Map;

public interface Message<T> extends Implementation<T> {
    String contentType();

    Map<String, Object> headers();

    Endpoint endpoint();

    <V> V body(Class V);

    boolean acknowledge() throws Exception;
}
