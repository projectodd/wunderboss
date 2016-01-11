/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.web.undertow.async.websocket;


import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.projectodd.wunderboss.web.async.WebsocketUtil;
import org.projectodd.wunderboss.web.async.websocket.WebsocketChannelSkeleton;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UndertowWebsocketChannel extends WebsocketChannelSkeleton {

    public UndertowWebsocketChannel(final OnOpen onOpen,
                                    final OnError onError,
                                    final OnClose onClose,
                                    final OnMessage onMessage) {
        super(onOpen, onError, onClose, onMessage);
    }

    @Override
    public UndertowEndpoint endpoint() {
        return new UndertowEndpoint() {
            @Override
            public void onMessage(WebSocketChannel _, Object message) {
               notifyMessage(message);
            }

            @Override
            public void onOpen(WebSocketChannel baseChannel,
                               WebSocketHttpExchange exchange) {
                setUnderlyingChannel(baseChannel);
                notifyOpen(exchange);
            }

            @Override
            public void onClose(WebSocketChannel _, CloseMessage message) {
                notifyClose(message.getCode(), message.getReason());
            }

            @Override
            public void onError(WebSocketChannel _, Throwable error) {
                notifyError(error);
                maybeCloseOnError(error);
            }
        };
    }

    @Override
    public void setUnderlyingChannel(final Object channel) {
        this.underlyingChannel = (WebSocketChannel) channel;
        setTimeoutOnUnderlyingChannel();
    }

    @Override
    public boolean isOpen() {
        return this.underlyingChannel != null &&
                this.underlyingChannel.isOpen();
    }

    @Override
    public boolean send(final Object message,
                        final boolean shouldClose,
                        final OnComplete onComplete) throws Exception {
        if (!isOpen()) {
            return false;
        }

        final WebSocketCallback<Void> callback = new WebSocketCallback<Void>() {
            @Override
            public void complete(WebSocketChannel channel, Void context) {
                if (shouldClose) {
                    close();
                }
                notifyComplete(onComplete, null);
            }

            @Override
            public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
                notifyComplete(onComplete, throwable);
                maybeCloseOnError(throwable);
            }
        };

        if (message == null) {
            callback.complete(this.underlyingChannel, null);
        } else if (message instanceof String) {
            WebSockets.sendText((String) message,
                    this.underlyingChannel,
                    callback);
        } else if (message instanceof byte[]) {
            WebSockets.sendBinary(ByteBuffer.wrap((byte[]) message),
                    this.underlyingChannel,
                    callback);
        } else {
            throw WebsocketUtil.wrongMessageType(message.getClass());
        }

        return true;
    }

    @Override
    public void close() {
        if (isOpen()) {
            try {
                this.underlyingChannel.sendClose();
            } catch (IOException _) {}
        }
        notifyClose(CloseMessage.NORMAL_CLOSURE, "");
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
        setTimeoutOnUnderlyingChannel();
    }

    private void setTimeoutOnUnderlyingChannel() {
        if (this.underlyingChannel != null &&
                this.timeout >= 0) {
            this.underlyingChannel.setIdleTimeout(timeout);
        }
    }
    protected void maybeCloseOnError(Throwable error) {
        if (error instanceof IOException) {
            close();
        }
    }

    private WebSocketChannel underlyingChannel;
    private long timeout = -1;
}
