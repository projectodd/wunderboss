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

package org.projectodd.wunderboss.web.async;

import javax.websocket.MessageHandler;

public class WebsocketUtil {

    // Overcomes the problem of Clojure's reify being incompatible with
    // generics and Undertow's dependence on ParameterizedType
    static public MessageHandler.Whole<String> createTextHandler(final MessageHandler.Whole proxy) {
        return new MessageHandler.Whole<String>() {
            public void onMessage(String msg) {
                proxy.onMessage(msg);
            }};
    }

    // The binary version of createTextHandler
    static public MessageHandler.Whole<byte[]> createBinaryHandler(final MessageHandler.Whole proxy) {
        return new MessageHandler.Whole<byte[]>() {
            public void onMessage(byte[] msg) {
                proxy.onMessage(msg);
            }};
    }

    static public RuntimeException wrongMessageType(Class clazz) {
        return new IllegalArgumentException("message is neither a String or byte[], but is " +
                                                   clazz.getName());
    }

    static public void notifyComplete(Channel channel, Channel.OnComplete callback, Throwable error) {
        if (callback != null) {
            try {
                callback.handle(error);
            } catch (Exception e) {
                channel.notifyError(e);
            }
        } else if (error != null) {
            channel.notifyError(error);
        }
    }
}
