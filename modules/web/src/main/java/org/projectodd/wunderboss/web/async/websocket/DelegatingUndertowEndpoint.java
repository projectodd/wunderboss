package org.projectodd.wunderboss.web.async.websocket;

import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;

public class DelegatingUndertowEndpoint implements UndertowEndpoint {


    private UndertowEndpoint endpoint;

    @Override
    public void onMessage(WebSocketChannel channel, Object message) {
        if (endpoint != null) {
            endpoint.onMessage(channel, message);
        } else {
            closeChannel(channel);
        }
    }

    @Override
    public void onOpen(WebSocketChannel channel, WebSocketHttpExchange exchange) {
        if (endpoint != null) {
            endpoint.onOpen(channel, exchange);
        } else {
            closeChannel(channel);
        }
    }

    @Override
    public void onClose(WebSocketChannel channel, CloseMessage message) {
        if (endpoint != null) {
            endpoint.onClose(channel, message);
        }
    }

    @Override
    public void onError(WebSocketChannel channel, Throwable error) {
        if (endpoint != null) {
            endpoint.onError(channel, error);
        } else {
            closeChannel(channel);
        }
    }

    public void setEndpoint(UndertowEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    void closeChannel(WebSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close websocket", e);
        }
    }
}
