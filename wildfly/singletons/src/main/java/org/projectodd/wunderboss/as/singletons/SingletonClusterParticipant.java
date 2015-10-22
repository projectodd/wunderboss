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

package org.projectodd.wunderboss.as.singletons;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.projectodd.wunderboss.ec.ClusterParticipant;

public class SingletonClusterParticipant implements ClusterParticipant, Service<Void> {

    @Override
    public boolean isMaster() {
        return this.isMaster;
    }

    @Override
    public void whenMasterAcquired(Runnable r) {
        this.acquiredMasterCallback = r;
    }

    @Override
    public void whenMasterLost(Runnable r) {
        this.lostMasterCallback = r;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.isMaster = true;
        if (this.acquiredMasterCallback != null) {
            this.acquiredMasterCallback.run();
        }
    }

    @Override
    public void stop(StopContext context) {
        this.isMaster = false;
        if (this.lostMasterCallback != null) {
            this.lostMasterCallback.run();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private boolean isMaster = false;
    private Runnable acquiredMasterCallback;
    private Runnable lostMasterCallback;

    private static final Logger log = Logger.getLogger(SingletonClusterParticipant.class);
}
