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

import org.projectodd.wunderboss.codecs.Codec;
import org.projectodd.wunderboss.codecs.Codecs;

import java.util.Map;

public interface Queue extends Destination {
    class RespondOption extends ListenOption {
        public static final RespondOption TTL = opt("ttl", 60000, RespondOption.class);
    }

    Listener respond(MessageHandler handler,
                     Codecs codecs,
                     Map<ListenOption, Object> options) throws Exception;


    class RequestOption extends SendOption {}

    Response request(Object content, Codec codec, Codecs codecs,
                     Map<MessageOpOption, Object> options) throws Exception;
}
