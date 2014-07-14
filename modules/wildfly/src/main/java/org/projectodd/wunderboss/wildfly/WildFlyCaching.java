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

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.caching.InfinispanCaching;
import org.projectodd.wunderboss.caching.Config;
import org.projectodd.wunderboss.wildfly.ClusterUtils;

import java.util.Map;


public class WildFlyCaching extends InfinispanCaching {

    public WildFlyCaching(String name, Options<CreateOption> options) {
        super(name, options);
    }

    public synchronized EmbeddedCacheManager manager() {
        if (this.manager == null) {
            this.manager = new DefaultCacheManager(getGlobalConfiguration(), Config.uration(this.options));
        }
        return this.manager;
    }

    protected Options<CreateOption> validate(Map<CreateOption,Object> options) {
        Options<CreateOption> result = new Options<CreateOption>(options);
        String mode = result.getString(CreateOption.MODE);
        // Default mode when in a cluster
        if (mode == null && ClusterUtils.inCluster()) {
            result.put(CreateOption.MODE, "DIST_SYNC");
        }
        return result;
    }

    // GlobalConfiguration getGlobalConfiguration() {
    //     ServiceRegistry serviceRegistry = (ServiceRegistry) WunderBoss.options().get("service-registry");
    //     EmbeddedCacheManagerConfiguration service = (EmbeddedCacheManagerConfiguration) serviceRegistry.getRequiredService(WildFlyService.WEB_CACHE_MANAGER_CONFIG).getValue();
    //     return service.getGlobalConfiguration();
    // }

    // GlobalConfiguration getGlobalConfiguration() {
    //     ServiceRegistry serviceRegistry = (ServiceRegistry) WunderBoss.options().get("service-registry");
    //     DefaultCacheContainer cm = (DefaultCacheContainer) serviceRegistry.getRequiredService(WildFlyService.WEB_CACHE_MANAGER).getValue();
    //     GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
    //     return builder.read(cm.getCacheManagerConfiguration())
    //         .classLoader(Thread.currentThread().getContextClassLoader())
    //         .build();
    // }

    // This won't cluster inside wildfly, but nothing else will
    // either, and at least this won't toss exceptions like the failed
    // attempts above
    GlobalConfiguration getGlobalConfiguration() {
        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
        return builder.clusteredDefault().build();
    }
}
