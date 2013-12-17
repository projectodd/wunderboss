package org.projectodd.wunderboss;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class WunderBoss {

    public WunderBoss registerLanguage(String languageName, Language language) {
        language.initialize(this);
        languages.put(languageName, language);

        return this;
    }

    public Language getLanguage(String name) {
        Language language = languages.get(name);
        if (language == null) {
            throw new IllegalArgumentException("Unknown language: " + name);
        }
        return language;
    }

    public boolean hasLanguage(String name) {
        return languages.containsKey(name);
    }

    public WunderBoss registerComponent(String componentName, Component component) {
        for (String dependency : component.getLanguageDependencies()) {
            if (!hasLanguage(dependency)) {
                throw new IllegalStateException("Component " + componentName +
                        " requires the " + dependency + " language");
            }
        }
        for (String dependency : component.getComponentDependencies()) {
            if (!hasComponent(dependency)) {
                throw new IllegalStateException("Component " + componentName +
                        " requires the " + dependency + " component");
            }
        }
        component.setContainer(this);
        component.boot();
        components.put(componentName, component);

        return this;
    }

    public boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    public WunderBoss configure(String componentName, Map<String, Object> options) {
        configure(componentName, new Options(options));

        return this;
    }

    public WunderBoss configure(String componentName, Options options) {
        getComponent(componentName).configure(options);

        return this;
    }

    public void stop() {
        for (Component component : components.values()) {
            component.shutdown();
        }
    }


    public Application newApplication(String languageName) {
        return newApplication(languageName, this.getClass().getClassLoader(), new Options());
    }

    public Application newApplication(String languageName, Map<String, Object> options) {
        return newApplication(languageName, this.getClass().getClassLoader(), new Options(options));
    }

    public Application newApplication(String languageName, ClassLoader classLoader, Options options) {
        return new Application(this, getLanguage(languageName), classLoader, options);
    }

    public Component getComponent(String name) {
        Component component = components.get(name);
        if (component == null) {
            throw new IllegalArgumentException("Unknown component: " + name);
        }
        return component;
    }

    public Logger getLogger(String name) {
        return Logger.getLogger(name);
    }

    public void setLogLevel(String level) {
        LogManager.getRootLogger().setLevel(Level.toLevel(level));
    }

    private Map<String, Language> languages = new HashMap<>();
    private Map<String, Component> components = new HashMap<>();

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}
