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

package org.projectodd.wunderboss.web.async;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.WunderBoss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IdleChannelReaper implements Runnable {

    public final static IdleChannelReaper INSTANCE = new IdleChannelReaper();

    @Override
    public void run() {
        log.debug("starting idle channel reaper thread");
        while(this.running) {
            try {
                checkChannels();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }

            if (Thread.currentThread().isInterrupted()) {
                stop();
            }
        }
    }

    private synchronized void checkChannels() {
        List<Channel> removals = new ArrayList<>();
        for (Channel each : channels) {
            if (each.closeIfIdleTimeoutExpired()) {
                log.debug("closed idle " + each);
                removals.add(each);
            }
        }

        channels.removeAll(removals);
    }

    private void start() {
        if (!this.running) {
            (new Thread(this, "idle-channel-reaper")).start();
            this.running = true;
            WunderBoss.addShutdownAction(new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            });
        }
    }

    public void stop() {
        log.debug("shutting down");
        this.running = false;
    }

    public synchronized void watchChannel(Channel channel) {
        log.debug("watching for idleness: " + channel);
        this.channels.add(channel);
        start();
    }

    private boolean running = false;
    private final Set<Channel> channels = new HashSet<>();
    private final Logger log = Logger.getLogger("org.projectodd.wunderboss.web.async");
}
