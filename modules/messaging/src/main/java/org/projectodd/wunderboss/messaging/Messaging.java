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

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

import java.util.Map;

public interface Messaging extends Component {

    class CreateOption extends Option {
        public static final CreateOption HOST = opt("host", CreateOption.class);
        public static final CreateOption PORT = opt("port", CreateOption.class);
    }

    class CreateQueueOption extends Option {
        public static final CreateQueueOption CONTEXT = opt("context", CreateQueueOption.class);
        public static final CreateQueueOption DURABLE   = opt("durable", true, CreateQueueOption.class);
        public static final CreateQueueOption SELECTOR  = opt("selector", CreateQueueOption.class);
    }

    Queue findOrCreateQueue(String name,
                            Map<CreateQueueOption, Object> options) throws Exception;

    class CreateTopicOption extends Option {
        public static final CreateTopicOption CONTEXT = opt("context", CreateTopicOption.class);
    }

    Topic findOrCreateTopic(String name,
                            Map<CreateTopicOption, Object> options) throws Exception;

    class CreateContextOption extends Option {
        public static final CreateContextOption HOST = opt("host", CreateContextOption.class);
        public static final CreateContextOption PORT = opt("port", 5445, CreateContextOption.class);
        public static final CreateContextOption CLIENT_ID = opt("client_id", CreateContextOption.class);
        public static final CreateContextOption USERNAME = opt("username", CreateContextOption.class);
        public static final CreateContextOption PASSWORD = opt("password", CreateContextOption.class);
        public static final CreateContextOption REMOTE_TYPE = opt("remote_type", CreateContextOption.class);
        public static final CreateContextOption MODE = opt("mode", Context.Mode.AUTO_ACK, CreateContextOption.class);

        /**
         * If true, an xa Context is returned.
         */
        public static final CreateContextOption XA = opt("xa", false, CreateContextOption.class);

        public static final CreateContextOption RECONNECT_RETRY_INTERVAL =
                opt("reconnect_retry_interval", 2000, CreateContextOption.class);
        public static final CreateContextOption RECONNECT_RETRY_INTERVAL_MULTIPLIER =
                opt("reconnect_retry_interval_multiplier", 1.0, CreateContextOption.class);
        public static final CreateContextOption RECONNECT_MAX_RETRY_INTERVAL =
                opt("reconnect_max_retry_interval", 2000, CreateContextOption.class);
        public static final CreateContextOption RECONNECT_ATTEMPTS =
                opt("reconnect_attempts", 0, CreateContextOption.class);
    }

    Context createContext(Map<CreateContextOption, Object> options) throws Exception;
}
