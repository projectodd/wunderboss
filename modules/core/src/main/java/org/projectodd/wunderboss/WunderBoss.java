/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class WunderBoss {

    static {
        init();
    }

    private static void init() {
        classLoader = new DynamicClassLoader(WunderBoss.class.getClassLoader());
        locator = new ClassPathLocator(classLoader);
        options = new Options<>();
        options.put("root", ".");
        configureLogback();
    }

    private WunderBoss() {}

    public static <T extends Component> T findOrCreateComponent(Class<T> clazz) {
        return findOrCreateComponent(clazz, null, null);
    }

    public static <T extends Component> T findOrCreateComponent(Class<T> clazz, String name, Map<Object, Object> options) {
        if (name == null) {
            name = "default";
        }

        String fullName = clazz.getName() + ":" + name;
        T component = (T) components.get(fullName);
        if (component != null) {
            log.debug("Returning existing component for " + fullName + ", ignoring options.");
        } else {
            component = getComponentProvider(clazz, true).create(name, new Options<>(options));
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

    public static void registerComponentProvider(Class iface, ComponentProvider<?> provider) {
        componentProviders.put(iface, provider);
    }

    public static boolean providesComponent(Class<? extends Component> clazz) {
        return getComponentProvider(clazz, false) != null;
    }

    public static void shutdownAndReset() throws Exception {
        for (Runnable action : shutdownActions) {
            action.run();
        }
        shutdownActions.clear();

        for (Language language : languages.values()) {
            language.shutdown();
        }
        languages.clear();

        for (Component component : components.values()) {
            component.stop();
        }
        components.clear();

        shutdownWorkerPool();
    }

    public static void addShutdownAction(Runnable action) {
        shutdownActions.add(action);
    }

    private static <T extends Component> ComponentProvider<T> getComponentProvider(Class<T> iface, boolean throwIfMissing) {
        ComponentProvider<T> provider = componentProviders.get(iface);

        if (provider == null &&
                (provider = locator.findComponentProvider(iface)) != null) {
            registerComponentProvider(iface, provider);
        }

        if (throwIfMissing &&
                provider == null) {
            throw new IllegalArgumentException("Unknown component: " + iface.getName());
        }

        return provider;
    }

    public static boolean inContainer() {
        return inWildFly();
    }

    public static boolean inWildFly() {
        return options().containsKey("service-registry");
    }

    public static Logger logger(String name) {
        return LoggerFactory.getLogger(name);
    }

    public static Logger logger(Class clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void setLogLevel(String level) {
        Logger logger = logger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        try {
            logger.getClass().getClassLoader().loadClass("ch.qos.logback.classic.Logger");
            LogbackUtil.setLogLevel(logger, level);
        } catch (ClassNotFoundException e) {
            log.error("Failed to change root logging level - only supported when using logback");
        }
    }

    private static void configureLogback() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        ClassLoader cl = loggerFactory.getClass().getClassLoader();
        try {
            cl.loadClass("ch.qos.logback.classic.LoggerContext");
            LogbackUtil.configureLogback(loggerFactory);
        } catch (ClassNotFoundException ignored) {
            // we're not using logback
        }
    }

    public static void updateClassPath(List<URL> classpath) {
        for(URL each : classpath) {
            classLoader.addURL(each);
        }
    }

    public static void updateClassPath(URL url) {
        classLoader.addURL(url);
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

    public static synchronized ExecutorService workerPool() {
        if (workerExecutor == null) {
            final String deploymentName = options().getString("deployment-name", "wunderboss");
            workerExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicLong counter = new AtomicLong(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("%s-worker-%d",
                                                       deploymentName, counter.getAndIncrement()));
                }
            });
        }

        return workerExecutor;
    }

    public static synchronized void shutdownWorkerPool() {
        if (workerExecutor != null) {
            workerExecutor.shutdown();
            workerExecutor = null;
        }
    }

    private static Locator locator;
    private static Options<String> options;
    private static final Map<String, Language> languages = new HashMap<>();
    private static final Map<Class, ComponentProvider> componentProviders = new HashMap<>();
    private static final Map<String, Component> components = new HashMap<>();
    private static final List<Runnable> shutdownActions = new ArrayList<>();
    private static DynamicClassLoader classLoader;
    private static ExecutorService workerExecutor;
    private static final Logger log = logger(WunderBoss.class);

}
