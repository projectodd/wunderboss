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

package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Option;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;

import java.util.Map;

public interface Destination {
    String name();

    class ListenOption extends Option {
        public static final ListenOption CONNECTION = opt("connection", ListenOption.class);
        public static final ListenOption CONCURRENCY = opt("concurrency", 1, ListenOption.class);
        public static final ListenOption SELECTOR    = opt("selector", ListenOption.class);
        public static final ListenOption TRANSACTED = opt("transacted", true, ListenOption.class);
    }

    Listener listen(MessageHandler handler,
                    Codecs codecs,
                    Map<ListenOption, Object> options) throws Exception;

    class MessageOpOption extends Option {
        public static final MessageOpOption CONNECTION = opt("connection", MessageOpOption.class);
        public static final MessageOpOption SESSION = opt("session", MessageOpOption.class);
    }

    class SendOption extends MessageOpOption {
        public static final SendOption PRIORITY   = opt("priority", 4, SendOption.class); //TODO: 4 is JMS specific?
        public static final SendOption TTL        = opt("ttl", 0, SendOption.class);
        public static final SendOption PERSISTENT = opt("persistent", true, SendOption.class);
        public static final SendOption PROPERTIES = opt("properties", SendOption.class);
    }

    void send(Object content, Codec codec, Map<MessageOpOption, Object> options) throws Exception;

    class ReceiveOption extends MessageOpOption {
        public static final ReceiveOption TIMEOUT  = opt("timeout", 10000, ReceiveOption.class);
        public static final ReceiveOption SELECTOR = opt("selector", ReceiveOption.class);
    }

    /**
     *
     * @param endpoint
     * @param options
     * @return the Message, or null on timeout
     * @throws Exception
     */
    Message receive(Codecs codecs, Map<MessageOpOption, Object> options) throws Exception;

    void stop() throws Exception;
}
