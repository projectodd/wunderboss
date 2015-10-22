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
package org.projectodd.wunderboss.as.singletons;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ec.DaemonContext;
import org.projectodd.wunderboss.ec.ImmediateContext;

public class SingletonsServiceActivator implements ServiceActivator {
    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) {
        final ServiceRegistry registry = serviceActivatorContext.getServiceRegistry();
        final ServiceTarget target = serviceActivatorContext.getServiceTarget();

        WunderBoss.registerComponentProvider(ImmediateContext.class,
                                             new ImmediateContextProvider(registry, target));
        WunderBoss.registerComponentProvider(DaemonContext.class,
                                             new DaemonContextProvider(registry, target));
    }
}
