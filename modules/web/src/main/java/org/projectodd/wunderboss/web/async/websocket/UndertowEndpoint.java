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

package org.projectodd.wunderboss.web.async.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public interface UndertowEndpoint {
    void onMessage (WebSocketChannel channel, Object message);
    void onOpen    (WebSocketChannel channel, WebSocketHttpExchange exchange);
    void onClose   (WebSocketChannel channel, CloseMessage message);
    void onError   (WebSocketChannel channel, Throwable error);
}
