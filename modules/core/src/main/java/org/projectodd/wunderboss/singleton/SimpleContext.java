/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

public class SimpleContext implements SingletonContext, ClusterChangeCallback {

    public SimpleContext(String name, ClusterParticipant participant, Options<CreateOption> options) {
        this.name = name;
        if (participant != null) {
            participant.setClusterChangeCallback(this);
        }
        this.clusterParticipant = participant;
    }

    @Override
    public SingletonContext setRunnable(Runnable r) {
        this.runnable = r;

        return this;
    }

    @Override
    public void run() {
        if (this.clusterParticipant == null ||
                this.clusterParticipant.isMaster()) {
            this.runnable.run();
        }
    }

    @Override
    public void clusterChanged(boolean wasMaster, boolean isMaster) {
        //don't care
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public boolean isRunning() {
        return this.clusterParticipant != null;
    }

    @Override
    public String name() {
        return this.name;
    }

    protected Runnable runnable() {
        return this.runnable;
    }

    protected ClusterParticipant clusterParticipant() {
        return this.clusterParticipant;
    }

    private final String name;
    private final ClusterParticipant clusterParticipant;
    private Runnable runnable;
}
