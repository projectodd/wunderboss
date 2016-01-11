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

package org.projectodd.wunderboss.ec;

import org.projectodd.wunderboss.WunderBoss;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class ConcreteDaemonContext extends ConcreteExecutionContext implements DaemonContext {

    public ConcreteDaemonContext(final String name,
                                 final ClusterParticipant participant,
                                 final boolean singleton,
                                 final long threadJoinTimeout) {
        super(name, participant, singleton);
        this.threadTimeout = threadJoinTimeout;
    }

    /**
     * Wraps the given action in a Runnable that starts a supervised thread.
     */
    @Override
    public void setAction(final Runnable action) {
        super.setAction(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    thread = new Thread(wrappedAction(action), "wunderboss-daemon-thread[" + name + "]");
                    thread.start();
                    isRunning = true;
                }
            }
        });
    }

    private Runnable wrappedAction(final Runnable action) {
        return new Runnable() {
            @Override
            public void run() {
                Throwable error = null;
                try {
                    action.run();
                } catch (Throwable t) {
                    error = t;
                } finally {
                    isRunning = false;
                }

                if (error != null) {
                    isStarted = false;
                    errorCallback.notify(name, error);
                }
            }
        };
    }
    
    /**
     * This runnable will be called when the daemon context stops, but only if the daemon has ran.
     */
    @Override
    public void setStopCallback(StopCallback c) {
        this.stopCallback = c;
    }

    /**
     * Called when an uncaught error occurs on the daemon thread.
     */
    @Override
    public void setErrorCallback(ErrorCallback c) {
        this.errorCallback = c;
    }

    @Override
    public boolean isStarted() {
        return this.isStarted;
    }

    @Override
    public void start() {
        if (!isRunning()) {
            this.isStarted = true;
            run();
        }
    }

    @Override
    public synchronized void stop() throws TimeoutException, InterruptedException {
        if (isStarted()) {
            if (this.stopCallback != null) {
                this.stopCallback.notify(this.name);
            }

            if (isRunning()) {
                this.isRunning = false;

                this.thread.join(threadTimeout);
                if (this.thread.isAlive()) {
                    throw new TimeoutException("Gave up after " + this.threadTimeout + "ms waiting for " +
                                                       this.name + " daemon to exit");
                }
            }
            
            this.isStarted = false;
        }
    }

    private final long threadTimeout;
    private boolean isStarted = false;
    private StopCallback stopCallback;
    private ErrorCallback errorCallback = DEFAULT_ERROR_CALLBACK;
    private Thread thread;
}
