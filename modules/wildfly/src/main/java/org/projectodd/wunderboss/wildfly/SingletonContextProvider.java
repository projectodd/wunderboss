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

package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.AlwaysRunContext;
import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.SingletonContext;

public class SingletonContextProvider implements ComponentProvider<SingletonContext> {

    @Override
    public SingletonContext create(String name, Options options) {
        if (ClusterUtils.inCluster()) {
            return new ClusteredSingletonContext(name);
        } else {
            return new AlwaysRunContext(name);
        }
    }
}
