package org.projectodd.wunderboss;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jboss.logging.Logger;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WunderBoss {

    static {
        init();
    }

    private static void init() {
        classLoader = new DynamicClassLoader(WunderBoss.class.getClassLoader());
        locator = new ClassPathLocator(classLoader);
        options = new Options<>();
        options.put("root", ".");
    }

    private WunderBoss() {}

    public static Component findOrCreateComponent(String type) {
        return findOrCreateComponent(type, null, null);
    }

    public static Component findOrCreateComponent(String type, String name, Map<Object, Object> options) {
        if (name == null) {
            name = "default";
        }

        String fullName = type + ":" + name;
        Component component = components.get(fullName);
        if (component != null) {
            log.info("Returning existing component for " + fullName + ", ignoring options.");
        } else {
            component = getComponentProvider(type, true).create(name, new Options<>(options));
            components.put(fullName, component);
        }

        return component;
    }

    public static Language findLanguage(String name) {
        return findLanguage(name, true);
    }

    private static Language findLanguage(String name, boolean throwIfMissing) {
        Language language = languages.get(name);

        if (language == null &&
                (language = locator.findLanguage(name)) != null) {
            registerLanguage(name, language);
        }

        if (throwIfMissing &&
                language == null) {
            throw new IllegalArgumentException("Unknown language: " + name);
        }
        return language;
    }


    public static void registerLanguage(String languageName, Language language) {
        language.initialize();
        languages.put(languageName, language);
    }

    public static boolean providesLanguage(String name) {
        return (findLanguage(name, false) != null);
    }

    public static void registerComponentProvider(String type, ComponentProvider provider) {
        componentProviders.put(type, provider);
    }

    public static boolean providesComponent(String type) {
        return getComponentProvider(type, false) != null;
    }

    public static void stop() throws Exception {
        for (Component component : components.values()) {
            component.stop();
        }

        for (Language language : languages.values()) {
            language.shutdown();
        }
    }

    private static ComponentProvider getComponentProvider(String type, boolean throwIfMissing) {
        ComponentProvider provider = componentProviders.get(type);

        if (provider == null &&
                (provider = locator.findComponentProvider(type)) != null) {
            registerComponentProvider(type, provider);
        }

        if (throwIfMissing &&
                provider == null) {
            throw new IllegalArgumentException("Unknown component: " + type);
        }

        return provider;
    }

    public static Logger logger(String name) {
        return Logger.getLogger(name);
    }

    public static void setLogLevel(String level) {
        LogManager.getRootLogger().setLevel(Level.toLevel(level));
    }

    public static void updateClassPath(List<URL> classpath) {
        for(URL each : classpath) {
            classLoader.addURL(each);
        }
    }

    public static ClassLoader classLoader() {
        return classLoader;
    }

    public static Locator locator() {
        return locator;
    }

    public static void setLocator(Locator loc) {
        locator = loc;
    }

    public static synchronized Options options() {
        return options;
    }

    public static synchronized void putOption(String k, Object v) {
        options.put(k, v);
    }

    public static synchronized void mergeOptions(Options<String> other) {
        options = options.merge(other);
    }

    private static Locator locator;
    private static Options<String> options;
    private static final Map<String, Language> languages = new HashMap<>();
    private static final Map<String, ComponentProvider> componentProviders = new HashMap<>();
    private static final Map<String, Component> components = new HashMap<>();
    private static DynamicClassLoader classLoader;
    private static final Logger log = Logger.getLogger(WunderBoss.class);

}
