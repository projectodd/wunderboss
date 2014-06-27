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

import org.projectodd.wunderboss.Options;

import java.util.concurrent.TimeoutException;

public class DaemonContext extends SimpleContext {

    public DaemonContext(String name, ClusterParticipant participant, Options<CreateOption> options) {
        super(name, participant, options);
        this.threadTimeout = options.getLong(CreateOption.DAEMON_THREAD_JOIN_TIMEOUT);
        this.stopCallback = (Runnable)options.get(CreateOption.DAEMON_STOP_CALLBACK);
    }

    @Override
    public synchronized void run() {
        if (!this.running) {
            this.thread = new Thread(runnable(), "singleton-thread[" + name() + "]");
            this.thread.start();
            this.running = true;
        }
    }


    @Override
    public void clusterChanged(boolean wasMaster, boolean isMaster) {
        if (!wasMaster && isMaster) {
            run();
        }
    }

    @Override
    public void start() {
        if (clusterParticipant() == null ||
                clusterParticipant().isMaster()) {
            run();
        }
    }

    @Override
    public synchronized void stop() throws TimeoutException, InterruptedException {
        if (this.running) {
            if (this.stopCallback != null) {
                this.stopCallback.run();
            }

            this.thread.join(this.threadTimeout);
            if (this.thread.isAlive()) {
                throw new TimeoutException("Gave up after " + this.threadTimeout + " waiting for " +
                                                   name() + " daemon to exit.");
            }

            this.running = false;
        }
    }

    private final long threadTimeout;
    private final Runnable stopCallback;
    private Thread thread;
    private boolean running = false;
}
