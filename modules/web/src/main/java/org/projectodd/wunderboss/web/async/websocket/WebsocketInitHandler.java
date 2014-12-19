package org.projectodd.wunderboss.web.async.websocket;

import io.undertow.websockets.spi.WebSocketHttpExchange;

public interface WebsocketInitHandler {
    /**
     *
     * @return true if the ws request is valid
     */
    boolean shouldConnect(WebSocketHttpExchange exchange, DelegatingUndertowEndpoint endpoint);
}
