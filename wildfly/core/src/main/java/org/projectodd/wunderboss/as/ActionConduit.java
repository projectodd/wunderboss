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

package org.projectodd.wunderboss.as;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ActionConduit {
    public synchronized void close() {
        if (!queue.isEmpty()) {
            throw new RuntimeException("Can't close non-empty conduit");
        }
        this.open = false;
    }

    public synchronized boolean add(Runnable action) {
        if (!isOpen()) {

            return false;
        }

        this.queue.add(action);

        return true;
    }

    public synchronized Runnable poll() {
        return this.queue.poll();
    }

    public synchronized boolean isOpen() {
        return this.open;
    }

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private boolean open = true;
}
