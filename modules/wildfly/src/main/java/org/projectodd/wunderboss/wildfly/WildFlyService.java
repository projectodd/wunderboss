package org.projectodd.wunderboss.wildfly;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ruby.rack.RackComponent;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;

public class WildFlyService implements Service<WildFlyService> {

    public WildFlyService(String deploymentName, ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.moduleName = deploymentName;

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
        container.registerComponent("web", new WildFlyWebComponent(serviceRegistry));
        container.registerComponent("rack", new RackComponent());
    }

    @Override
    public void start(StartContext context) throws StartException {
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
        container.stop();
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private WunderBoss container;
    private Application application;
    private ServiceRegistry serviceRegistry;
    private String moduleName;
}
