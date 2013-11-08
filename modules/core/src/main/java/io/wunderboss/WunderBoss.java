package io.wunderboss;

import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class WunderBoss {

    public void registerLanguage(String languageName, Language language) {
        language.initialize(this);
        languages.put(languageName, language);
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

    public void registerComponent(String componentName, Component component) {
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
    }

    public boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    public void configure(String componentName, Map<String, Object> options) {
        configure(componentName, new Options(options));
    }

    public void configure(String componentName, Options options) {
        getComponent(componentName).configure(options);
    }

    public ComponentInstance start(String componentName) {
        return start(componentName, new HashMap<String, Object>());
    }

    public ComponentInstance start(String componentName, Map<String, Object> options) {
        return start(componentName, new Options(options));
    }

    public ComponentInstance start(String componentName, Options options) {
        return getComponent(componentName).start(options);
    }

    public void stop() {
        for (Component component : components.values()) {
            component.shutdown();
        }
    }

    private Component getComponent(String name) {
        Component component = components.get(name);
        if (component == null) {
            throw new IllegalArgumentException("Unknown component: " + name);
        }
        return component;
    }

    private Map<String, Language> languages = new HashMap<>();
    private Map<String, Component> components = new HashMap<>();

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}
