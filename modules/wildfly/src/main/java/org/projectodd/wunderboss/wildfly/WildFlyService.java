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

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.wunderboss.Utils;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ruby.RubyLocator;
import org.wildfly.extension.undertow.UndertowService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WildFlyService implements Service<WildFlyService> {

    public static ServiceName serviceName(String deploymentName) {
        ServiceName parentServiceName = ServiceName.JBOSS.append("deployment").append("unit").append(deploymentName);

        return ServiceName.of(parentServiceName, "wunderboss");
    }

    public WildFlyService(String deploymentName, ServiceRegistry registry) {
        this.deploymentName = deploymentName;
        this.registry = registry;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Properties properties = new Properties();
        String configName = deploymentName.replace(".jar", ".properties");
        String configPath = System.getProperty("jboss.server.base.dir") + File.separator + "deployments" + File.separator + configName;
        log.debug("!!! Looking for config file at " + configPath);
        File configFile = new File(configPath);
        if (configFile.exists()) {
            log.debug("!!! Found config file");
            try {
                properties.load(new FileInputStream(configFile));
            } catch (Exception e) {
                log.error("Error loading config file: " + configPath);
                throw new StartException(e);
            }
        }
        WunderBoss.putOption("deployment-name", this.deploymentName);
        WunderBoss.putOption("service-registry", this.registry);
        WunderBoss.putOption("wildfly-service", this);
        WunderBoss.putOption("root", requiredProperty(properties, "root"));

        String language = requiredProperty(properties, "language");
        List<File> classpathAdditions = new ArrayList<>();

        if (language.equals("ruby")) {
            classpathAdditions.addAll(RubyLocator.locateLibs(properties.getProperty("jruby.home")));
        }

        if (properties.containsKey("classpath")) {
            classpathAdditions.addAll(Utils.classpathStringToFiles(properties.getProperty("classpath")));
        }

        try {
            ModuleUtils.addToModuleClasspath(Module.forClass(WildFlyService.class), classpathAdditions);
        } catch (IOException | ModuleLoadException e) {
            throw new StartException(e);
        }

        WunderBoss.registerComponentProvider(new WildflyWebProvider(undertowInjector.getValue()));
        WunderBoss.registerComponentProvider(new WildFlyMessagingProvider());
        WunderBoss.registerComponentProvider(new SingletonContextProvider());

        log.info("Initializing " + deploymentName + " as " + language);
        WunderBoss.findLanguage(language)
                .eval(requiredProperty(properties, "init"));
    }

    private String requiredProperty(Properties properties, String key) {
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        } else {
            throw new IllegalArgumentException("Required option " + key + " not provided.");
        }
    }

    @Override
    public void stop(StopContext context) {
        log.debug("!!! Stopping WunderBoss application");
        try {
            WunderBoss.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Injector<UndertowService> getUndertowInjector() {
        return undertowInjector;
    }

    public Injector<ChannelFactory> getChannelFactoryInjector() {
        return channelFactoryInjector;
    }

    public ChannelFactory channelFactory() {
        return this.channelFactoryInjector.getValue();
    }

    private final String deploymentName;
    private final ServiceRegistry registry;
    private InjectedValue<UndertowService> undertowInjector = new InjectedValue<>();
    private InjectedValue<ChannelFactory> channelFactoryInjector = new InjectedValue<>();

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");
}
