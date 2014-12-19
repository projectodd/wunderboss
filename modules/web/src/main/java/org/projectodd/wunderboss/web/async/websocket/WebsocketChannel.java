package org.projectodd.wunderboss.web.async.websocket;

import org.projectodd.wunderboss.web.async.Channel;

public interface WebsocketChannel extends Channel {
    Object getEndpoint();
    void setUnderlyingChannel(Object channel);

    interface OnMessage {
        void handle(WebsocketChannel channel, Object message);
    }

    interface OnError {
        void handle(WebsocketChannel channel, Throwable error);
    }
}
