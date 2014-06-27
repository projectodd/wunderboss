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

package org.projectodd.wunderboss.singleton;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

public interface SingletonContext extends Component, Runnable {

    class CreateOption extends Option {
        /**
         * The context will call the runnable as soon as it becomes master, and run the runnable in a separate thread.
         */
        public static final CreateOption DAEMON = opt("daemon", false, CreateOption.class);

        /**
         * The amount of time a daemon context will wait for its thread to exit, in millis.
         */
        public static final CreateOption DAEMON_THREAD_JOIN_TIMEOUT = opt("daemon_thread_join_timeout", 30000, CreateOption.class);

        /**
         * This runnable will be called when the daemon context stops, but only if the daemon is actually running.
         */
        public static final CreateOption DAEMON_STOP_CALLBACK = opt("daemon_stop_callback", CreateOption.class);
    }

    SingletonContext setRunnable(Runnable r);

}
