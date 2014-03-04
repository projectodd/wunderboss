package org.projectodd.wunderboss;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jboss.logging.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WunderBoss {

    public WunderBoss() {
        this(new Options().put("root", "."));
    }

    public WunderBoss(Map<String, Object> options) {
        this(new Options(options));
    }

    public WunderBoss(Options options) {
        this(options, WunderBoss.class.getClassLoader(), new ClassPathLocator());
    }

    public WunderBoss(Options options, ClassLoader loader, Locator locator) {
        this.options = options;
        this.classLoader = new DynamicClassLoader(loader);
        locator.setClassLoader(this.classLoader);
        this.locator = locator;

        updateClassPath();
    }

    public WunderBoss mergeOptions(Options opts) {
        this.options.merge(opts);
        updateClassPath();

        return this;
    }

    public WunderBoss registerLanguage(String languageName, Language language) {
        language.initialize(this);
        languages.put(languageName, language);

        return this;
    }

    public Language getLanguage(String name) {
        return getLanguage(name, true);
    }

    protected Language getLanguage(String name, boolean throwIfMissing) {
        Language language = languages.get(name);

        if (language == null &&
                (language = this.locator.findLanguage(name)) != null) {
            registerLanguage(name, language);
            language = languages.get(name);
        }

        if (throwIfMissing &&
                language == null) {
            throw new IllegalArgumentException("Unknown language: " + name);
        }
        return language;
    }


    public boolean hasLanguage(String name) {
        return (getLanguage(name, false) != null);
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
        return getComponent(name, false) != null;
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
        for (Application application : applications) {
            application.stop();
        }

        for (Component component : components.values()) {
            component.shutdown();
        }

        for (Language language : languages.values()) {
            language.shutdown();
        }
    }


    public Application newApplication(String languageName) {
        return newApplication(languageName, new Options());
    }

    public Application newApplication(String languageName, Map<String, Object> options) {
        return newApplication(languageName, new Options(options));
    }

    public Application newApplication(String languageName, Options options) {
        Application application = new Application(this, getLanguage(languageName), options);
        applications.add(application);
        return application;
    }

    public Component getComponent(String name) {
        return getComponent(name, true);
    }

    protected Component getComponent(String name, boolean throwIfMissing) {
        Component component = components.get(name);

        if (component == null &&
                (component = this.locator.findComponent(name)) != null) {
            registerComponent(name, component);
            component = components.get(name);
        }

        if (throwIfMissing &&
                component == null) {
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

    public Options options() {
        return this.options;
    }

    public ClassLoader classLoader() {
        return this.classLoader;
    }

    protected void updateClassPath() {
        List<URL> classpath =
                new ArrayList<>((List<URL>)this.options.get("classpath", Collections.EMPTY_LIST));
        for(URL each : classpath) {
            this.classLoader.addURL(each);
        }
    }

    private final Locator locator;
    private final Map<String, Language> languages = new HashMap<>();
    private final Map<String, Component> components = new HashMap<>();
    private final List<Application> applications = new ArrayList<>();
    private final Options options;
    private final DynamicClassLoader classLoader;

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}