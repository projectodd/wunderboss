package org.projectodd.wunderboss.wildfly;

import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.Utils;
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
        log.debug("!!! Looking for config file at " + configPath);
        File configFile = new File(configPath);
        if (configFile.exists()) {
            log.debug("!!! Found config file");
            try {
                properties.load(new FileInputStream(configFile));
            } catch (Exception e) {
                System.err.println("Error loading config file: " + e.getMessage());
            }
        }
        WunderBoss.putOption("root", requiredProperty(properties, "root"));

        if (properties.containsKey("classpath")) {
            WunderBoss.updateClassPath(Utils.classpathStringToURLS(properties.getProperty("classpath")));
        }

        String language = requiredProperty(properties, "language");

        if (language.equals("ruby")) {
            WunderBoss.registerLanguage("ruby", new WildFlyRubyLanguage(requiredProperty(properties, "jruby.home")));
        }
        WunderBoss.registerComponentProvider("web", new WildflyWebProvider(undertowInjector.getValue()));


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

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");
}
