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

package org.projectodd.wunderboss.as.singletons;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

import java.util.List;
import java.util.Random;

/*
 * WF 9+ provides a random impl, but we need our own to support WF8
 */
public class RandomSingletonElectionPolicy implements SingletonElectionPolicy {

    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public Node elect(List<Node> nodes) {
        int size = nodes.size();
        return (size > 0) ? nodes.get(this.random.nextInt(size)) : null;
    }
}
