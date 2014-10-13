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

import java.util.Map;

public interface Connection extends AutoCloseable {

    public static final Object XA = new Object();

    class CreateSessionOption extends Option {
        public static final CreateSessionOption MODE = opt("mode", Session.Mode.AUTO_ACK, CreateSessionOption.class);
    }

    Session createSession(Map<CreateSessionOption, Object> options) throws Exception;

    void addCloseable(AutoCloseable closeable);

    boolean isRemote();
}
