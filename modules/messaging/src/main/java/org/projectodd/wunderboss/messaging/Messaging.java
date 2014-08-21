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
        public static final CreateQueueOption CONNECTION = opt("connection", CreateQueueOption.class);
        public static final CreateQueueOption DURABLE   = opt("durable", true, CreateQueueOption.class);
        public static final CreateQueueOption SELECTOR  = opt("selector", CreateQueueOption.class);
    }

    Queue findOrCreateQueue(String name,
                            Map<CreateQueueOption, Object> options) throws Exception;

    class CreateTopicOption extends Option {
        public static final CreateTopicOption CONNECTION = opt("connection", CreateTopicOption.class);
    }

    Topic findOrCreateTopic(String name,
                            Map<CreateTopicOption, Object> options) throws Exception;

    class CreateConnectionOption extends Option {
        public static final CreateConnectionOption HOST = opt("host", CreateConnectionOption.class);
        public static final CreateConnectionOption PORT = opt("port", 5445, CreateConnectionOption.class);
        public static final CreateConnectionOption CLIENT_ID = opt("client_id", CreateConnectionOption.class);
        public static final CreateConnectionOption USERNAME = opt("username", CreateConnectionOption.class);
        public static final CreateConnectionOption PASSWORD = opt("password", CreateConnectionOption.class);
        public static final CreateConnectionOption REMOTE_TYPE = opt("remote_type", CreateConnectionOption.class);

        /**
         * If true, and xa connection is returned.
         */
        public static final CreateConnectionOption XA = opt("xa", false, CreateConnectionOption.class);

        public static final CreateConnectionOption RECONNECT_RETRY_INTERVAL =
                opt("reconnect_retry_interval", 2000, CreateConnectionOption.class);
        public static final CreateConnectionOption RECONNECT_RETRY_INTERVAL_MULTIPLIER =
                opt("reconnect_retry_interval_multiplier", 1.0, CreateConnectionOption.class);
        public static final CreateConnectionOption RECONNECT_MAX_RETRY_INTERVAL =
                opt("reconnect_max_retry_interval", 2000, CreateConnectionOption.class);
        public static final CreateConnectionOption RECONNECT_ATTEMPTS =
                opt("reconnect_attempts", 0, CreateConnectionOption.class);
    }

    Connection createConnection(Map<CreateConnectionOption, Object> options) throws Exception;

    Connection defaultConnection() throws Exception;
}
