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
import org.projectodd.wunderboss.codecs.Codecs;

import java.util.Map;

public interface Topic extends Destination {

    class SubscribeOption extends Option {
        public static final SubscribeOption CONTEXT = opt("context", SubscribeOption.class);
        public static final SubscribeOption SELECTOR = opt("selector", SubscribeOption.class);
        public static final SubscribeOption TRANSACTED = opt("transacted", true, SubscribeOption.class);
    }

    Listener subscribe(String id, MessageHandler handler,
                       Codecs codecs,
                       Map<SubscribeOption, Object> options) throws Exception;

    class UnsubscribeOption extends Option {
        public static final UnsubscribeOption CONTEXT = opt("context", UnsubscribeOption.class);
    }

    void unsubscribe(String id, Map<UnsubscribeOption, Object> options) throws Exception;
}
