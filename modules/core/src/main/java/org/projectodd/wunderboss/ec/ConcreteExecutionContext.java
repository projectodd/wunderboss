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

package org.projectodd.wunderboss.ec;

public abstract class ConcreteExecutionContext implements ExecutionContext {

    public ConcreteExecutionContext(final String name,
                                    final ClusterParticipant clusterParticipant,
                                    final boolean singleton) {
        this.name = name;
        this.clusterParticipant = clusterParticipant;
        this.singleton = singleton;

        clusterParticipant.whenMasterAcquired(new Runnable() {
            @Override
            public void run() {
                ConcreteExecutionContext.this.start();
            }
        });

        clusterParticipant.whenMasterLost(new Runnable() {
            @Override
            public void run() {
                try {
                    ConcreteExecutionContext.this.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });


    }

    @Override
    public void setAction(Runnable r) {
        this.action = r;
    }

    @Override
    public void run() {
        if (!this.singleton ||
                this.clusterParticipant.isMaster()) {
            this.action.run();
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() throws Exception {
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public String name() {
        return this.name;
    }

    protected boolean isRunning = false;
    protected Runnable action;
    protected final String name;
    protected final ClusterParticipant clusterParticipant;
    protected final boolean singleton;


}
