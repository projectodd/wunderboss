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

package org.projectodd.wunderboss;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ClassPathLocator implements Locator {

    public ClassPathLocator(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public synchronized Language findLanguage(String name) {
        String lang = findProviderClassName("language", name);
        if (lang != null) {
            return (Language)instantiate(lang);
        } else {
            return null;
        }
    }

    @Override
    public synchronized ComponentProvider findComponentProvider(Class<? extends Component> clazz) {
        Class<?> providerClass = findProviderClass("componentProvider", clazz);
        if (providerClass != null) {
            return (ComponentProvider)instantiate(providerClass);
        } else {
            return null;
        }
    }

    protected String findProviderClassName(String type, String name) {
        log.debug("Looking for provider " + type + ":" + name);
        try {
            Enumeration<URL> urls = getWunderBossPropertyUrls();

            while(urls.hasMoreElements()) {
                URL url = urls.nextElement();
                log.debug("checking " + url);

                Properties props = findPropertiesForUrl(url);

                String cName;
                if ((cName = props.getProperty(type + "." + name)) != null) {
                    return cName;
                }
            }
        } catch (IOException e) {
            //TODO: something better
            e.printStackTrace();
        }

        return null;
    }

    protected Class<?> findProviderClass(String type, Class<?> clazz) {
        log.debug("Looking for provider " + type + ":" + clazz.getName());
        try {
            Enumeration<URL> urls = getWunderBossPropertyUrls();

            while(urls.hasMoreElements()) {
                URL url = urls.nextElement();
                log.info("checking " + url);

                Properties props = findPropertiesForUrl(url);
                for(String each : props.stringPropertyNames()) {
                    log.info(each);
                    if (each.startsWith(type)) {
                        String providerClassName = props.getProperty(each);
                        if (providerClassName != null) {
                            Class providerClass = this.loader.loadClass(providerClassName);
                            if (clazz.isAssignableFrom(WunderBoss.getProvidedType(providerClass))) {

                                return providerClass;
                            }
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            //TODO: something better
            e.printStackTrace();
        }

        return null;
    }

    protected Enumeration<URL> getWunderBossPropertyUrls() throws IOException {
        return this.loader.getResources("META-INF/wunderboss.properties");
    }

    protected Properties findPropertiesForUrl(URL url) throws IOException {
        Properties props;

        if (this.propertiesCache.containsKey(url)) {
            props = this.propertiesCache.get(url);
            log.debug("Found CACHED props: " + props);
        } else {
            props = new Properties();
            InputStream propsStream = url.openStream();
            try {
                props.load(propsStream);
            } finally {
                propsStream.close();
            }
            this.propertiesCache.put(url, props);
            log.debug("Found props: " + props);
        }
        return props;
    }

    private Object instantiate(String cName) {
        try {
            Class clazz = this.loader.loadClass(cName);
            return instantiate(clazz);
        } catch (ClassNotFoundException e) {
            //TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    private Object instantiate(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            //TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    private ClassLoader loader;
    private final Map<URL, Properties> propertiesCache = new HashMap<>();
    private static final Logger log = Logger.getLogger(ClassPathLocator.class);
}
