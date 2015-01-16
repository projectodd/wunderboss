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
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UndertowWebsocketChannel extends WebsocketChannelSkeleton {

    public UndertowWebsocketChannel(final OnOpen onOpen,
                                    final OnClose onClose,
                                    final OnMessage onMessage,
                                    final OnError onError) {
        super(onOpen, onClose, onMessage, onError);
    }

    @Override
    public UndertowEndpoint endpoint() {
        final UndertowWebsocketChannel channel = this;
        return new UndertowEndpoint() {
            @Override
            public void onMessage(WebSocketChannel _, Object message) {
               notifyMessage(message);
            }

            @Override
            public void onOpen(WebSocketChannel baseChannel,
                               WebSocketHttpExchange exchange) {
                channel.setUnderlyingChannel(baseChannel);
                channel.notifyOpen(exchange);
            }

            @Override
            public void onClose(WebSocketChannel _, CloseMessage message) {
                notifyClose(message.getCode(), message.getReason());
            }

            @Override
            public void onError(WebSocketChannel _, Throwable error) {
                notifyError(error);
            }
        };
    }

    @Override
    public void setUnderlyingChannel(final Object channel) {
        this.underlyingChannel = (WebSocketChannel) channel;
    }

    @Override
    public boolean isOpen() {
        return this.underlyingChannel != null &&
                this.underlyingChannel.isOpen();
    }

    @Override
    public boolean send(final Object message, final boolean shouldClose) throws Exception {
        if (!isOpen()) {
            return false;
        }

        final WebSocketCallback<Void> callback = new WebSocketCallback<Void>() {
            @Override
            public void complete(WebSocketChannel channel, Void context) {
                if (shouldClose) {
                    try {
                        close();
                    } catch (IOException e) {
                        notifyError(e);
                    }
                }
            }

            @Override
            public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
                notifyError(throwable);
            }
        };
        if (message instanceof String) {
            WebSockets.sendText((String) message,
                    this.underlyingChannel,
                    callback);
        } else if (message instanceof byte[]) {
            WebSockets.sendBinary(ByteBuffer.wrap((byte[]) message),
                    this.underlyingChannel,
                    callback);
        } else {
            throw Util.wrongMessageType(message.getClass());
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            this.underlyingChannel.sendClose();
        }
    }

    private WebSocketChannel underlyingChannel;
}



