/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.websocket;

import java.nio.ByteBuffer;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.Buffers;
import org.xnio.Pooled;


public class UndertowWebsocket {

    public static HttpHandler create (final Endpoint endpoint, final HttpHandler next) {
        WebSocketConnectionCallback callback = new WebSocketConnectionCallback() {
                public void onConnect (WebSocketHttpExchange exchange, WebSocketChannel channel) {
                    endpoint.onOpen(channel, exchange);
                    channel.getReceiveSetter().set(new AbstractReceiveListener() {
                            protected void onError (WebSocketChannel channel, Throwable error) {
                                endpoint.onError(channel, error);
                            }
                            protected void onCloseMessage (CloseMessage message, WebSocketChannel channel) {
                                endpoint.onClose(channel, message);
                            }
                            protected void onFullTextMessage (WebSocketChannel channel, BufferedTextMessage message) {
                                endpoint.onMessage(channel, message.getData());
                            }
                            protected void onFullBinaryMessage (WebSocketChannel channel, BufferedBinaryMessage message) {
                                Pooled<ByteBuffer[]> pooled = message.getData();
                                try {
                                    ByteBuffer[] payload = pooled.getResource();
                                    endpoint.onMessage(channel, toArray(payload));
                                } finally {
                                    pooled.free();
                                }
                            }
                        });
                    channel.resumeReceives();
                }
            };
        HttpHandler fallback = next==null ? ResponseCodeHandler.HANDLE_404 : next;
        return new WebSocketProtocolHandshakeHandler(callback, fallback);
    }

    public static void send(final WebSocketChannel channel, final Object message, final WebSocketCallback<Void> callback) {
        if (message instanceof String) {
            WebSockets.sendText((String) message, channel, callback);
        } else {
            WebSockets.sendBinary(ByteBuffer.wrap((byte[]) message), channel, callback);
        }
    }

    // Lifted from Undertow's FrameHandler.java
    protected static byte[] toArray(ByteBuffer... payload) {
        if (payload.length == 1) {
            ByteBuffer buf = payload[0];
            if (buf.hasArray() && buf.arrayOffset() == 0 && buf.position() == 0) {
                return buf.array();
            }
        }
        int size = (int) Buffers.remaining(payload);
        byte[] data = new byte[size];
        for (ByteBuffer buf : payload) {
            buf.get(data);
        }
        return data;
    }
}
