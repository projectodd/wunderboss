/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
