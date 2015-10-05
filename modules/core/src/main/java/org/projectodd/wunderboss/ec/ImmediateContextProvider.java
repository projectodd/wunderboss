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

package org.projectodd.wunderboss.ec;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;

public class ImmediateContextProvider extends ExecutionContextProvider implements ComponentProvider<ImmediateContext> {
    @Override
    public ImmediateContext create(String name, Options options) {
        return new ImmediateContext(name,
                                    clusterParticipant(name),
                                    options.getBoolean(ExecutionContext.CreateOption.SINGLETON));
    }
}
