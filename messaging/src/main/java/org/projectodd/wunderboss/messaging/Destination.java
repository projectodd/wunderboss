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

package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Option;
import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;

import java.util.Map;

public interface Destination extends HasCloseables {
    String name();

    class ListenOption extends Option {
        public static final ListenOption CONTEXT     = opt("context", ListenOption.class);
        public static final ListenOption CONCURRENCY = opt("concurrency", ListenOption.class);
        public static final ListenOption SELECTOR    = opt("selector", ListenOption.class);
        public static final ListenOption MODE        = opt("mode", Context.Mode.TRANSACTED, ListenOption.class);
    }

    Listener listen(MessageHandler handler,
                    Codecs codecs,
                    Map<ListenOption, Object> options) throws Exception;

    class MessageOpOption extends Option {
        public static final MessageOpOption CONTEXT = opt("context", MessageOpOption.class);
    }

    class PublishOption extends MessageOpOption {
        public static final PublishOption PRIORITY   = opt("priority", 4, PublishOption.class); //TODO: 4 is JMS specific?
        public static final PublishOption TTL        = opt("ttl", 0, PublishOption.class);
        public static final PublishOption PERSISTENT = opt("persistent", true, PublishOption.class);
        public static final PublishOption PROPERTIES = opt("properties", PublishOption.class);
    }

    void publish(Object content, Codec codec, Map<MessageOpOption, Object> options) throws Exception;

    class ReceiveOption extends MessageOpOption {
        public static final ReceiveOption TIMEOUT  = opt("timeout", 10000, ReceiveOption.class);
        public static final ReceiveOption SELECTOR = opt("selector", ReceiveOption.class);
    }

    Message receive(Codecs codecs, Map<MessageOpOption, Object> options) throws Exception;

    int defaultConcurrency();

    void stop() throws Exception;
}
