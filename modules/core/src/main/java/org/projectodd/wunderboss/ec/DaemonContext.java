/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.ec;

import org.projectodd.wunderboss.WunderBoss;
import org.slf4j.Logger;

public interface DaemonContext extends ExecutionContext {

    ErrorCallback DEFAULT_ERROR_CALLBACK = new ErrorCallback() {
        @Override
        public void notify(String name, Throwable t) {
            LOG.error("Daemon " + name + " threw an unhandled exception", t);
        }
    };

    Logger LOG = WunderBoss.logger("org.projectodd.wunderboss.ec");

    class CreateOption extends ExecutionContext.CreateOption {
        /**
         * The amount of time a daemon context will wait for its thread to exit, in millis.
         */
        public static final CreateOption STOP_TIMEOUT = opt("stop_timeout", 30000L, CreateOption.class);

    }

    boolean isStarted();

    void setStopCallback(StopCallback c);

    void setErrorCallback(ErrorCallback c);

    interface StopCallback {
        void notify(String name);
    }

    interface ErrorCallback {
        void notify(String name, Throwable t);
    }
}
