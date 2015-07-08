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

package org.projectodd.wunderboss.as;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.singleton.ClusterParticipant;
import org.projectodd.wunderboss.singleton.DaemonContext;
import org.projectodd.wunderboss.singleton.SimpleContext;
import org.projectodd.wunderboss.singleton.SingletonContext;

public class SingletonContextProvider implements ComponentProvider<SingletonContext> {

    @Override
    public SingletonContext create(String name, Options options) {
        ClusterParticipant participant = null;
        if (ClusterUtils.inCluster()) {
            participant = new SingletonClusterParticipant(name);
        }

        if (options.getBoolean(SingletonContext.CreateOption.DAEMON)) {
            return new DaemonContext(name, participant, options);
        } else {
            return new SimpleContext(name, participant, options);
        }

    }
}
