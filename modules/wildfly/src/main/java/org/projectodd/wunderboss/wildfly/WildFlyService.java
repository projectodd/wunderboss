package org.projectodd.wunderboss.wildfly;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
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
        WunderBoss.mergeOptions(options);
        WunderBoss.registerLanguage("ruby", new WildFlyRubyLanguage(properties.getProperty("jruby.home")));
        WunderBoss.registerComponentProvider("web", new WildflyWebComponentProvider(undertowInjector.getValue()));

        //System.err.println("!!! Starting Ruby application");
        //TODO: call some init code here
    }

    @Override
    public void stop(StopContext context) {
        System.err.println("!!! Stopping Ruby application");
        WunderBoss.stop();
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Injector<UndertowService> getUndertowInjector() {
        return undertowInjector;
    }

    private final String deploymentName;
    private InjectedValue<UndertowService> undertowInjector = new InjectedValue<UndertowService>();
}
