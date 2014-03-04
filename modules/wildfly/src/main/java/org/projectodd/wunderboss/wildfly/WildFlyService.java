package org.projectodd.wunderboss.wildfly;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.wildfly.extension.undertow.UndertowService;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class WildFlyService implements Service<WildFlyService> {

    public WildFlyService(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Properties properties = new Properties();
        String configName = deploymentName.replace(".jar", ".properties");
        String configPath = System.getProperty("jboss.server.base.dir") + File.separator + "deployments" + File.separator + configName;
        System.err.println("!!! Looking for config file at " + configPath);
        File configFile = new File(configPath);
        if (configFile.exists()) {
            System.err.println("!!! Found config file");
            try {
                properties.load(new FileInputStream(configFile));
            } catch (Exception e) {
                System.err.println("Error loading config file: " + e.getMessage());
            }
        }
        Options options = new Options();
        options.put("root", properties.getProperty("root"));
        container = new WunderBoss(options);
        container.registerLanguage("ruby", new WildFlyRubyLanguage(properties.getProperty("jruby.home")));
        container.registerComponent("web", new WildFlyWebComponent(undertowInjector.getValue()));

        System.err.println("!!! Starting Ruby application");
        application = container.newApplication("ruby");
        application.start("rack");
    }

    @Override
    public void stop(StopContext context) {
        if (application != null) {
            System.err.println("!!! Stopping Ruby application");
            application.stop();
        }
        if (container != null) {
            container.stop();
        }
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Injector<UndertowService> getUndertowInjector() {
        return undertowInjector;
    }

    private final String deploymentName;
    private WunderBoss container;
    private Application application;
    private InjectedValue<UndertowService> undertowInjector = new InjectedValue<UndertowService>();
}
