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

    @Override
    public void setClassLoader(ClassLoader loader) {
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
    public synchronized Component findComponent(String name) {
        String comp = findProviderClassName("component", name);
        if (comp != null) {
            return (Component)instantiate(comp);
        } else {
            return null;
        }
    }

    protected String findProviderClassName(String type, String name) {
        log.debug("Looking for provider " + type + ":" + name);
        try {
            Enumeration<URL> urls =
                    this.loader.getResources("META-INF/wunderboss.properties");

            while(urls.hasMoreElements()) {
                URL url = urls.nextElement();
                log.debug("checking " + url);

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

    private Object instantiate(String cName) {
        try {
            Class clazz = this.loader.loadClass(cName);
            return clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            //TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    private ClassLoader loader;
    private final Map<URL, Properties> propertiesCache = new HashMap<>();
    private static final Logger log = Logger.getLogger(ClassPathLocator.class);
}
