/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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



import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class ClassPathLocator implements Locator {

    public ClassPathLocator(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public synchronized Language findLanguage(String name) {
        Class lang = languageClassFor(name);
        if (lang != null) {
            return (Language)instantiate(lang);
        } else {
            return null;
        }
    }

    @Override
    public synchronized ComponentProvider findComponentProvider(Class<? extends Component> clazz) {
        Class<?> providerClass = providerClassFor(clazz);
        if (providerClass != null) {
            return (ComponentProvider)instantiate(providerClass);
        } else {
            return null;
        }
    }

    protected Class languageClassFor(String lang) {
        try {
            return loadClassFromResourceFile("META-INF/wunderboss-languages/" + lang);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load language implementation for " + lang, e);
        }
    }

    protected Class<ComponentProvider> providerClassFor(Class clazz) {
        try {
            return (Class<ComponentProvider>)loadClassFromResourceFile("META-INF/wunderboss-providers/" + clazz.getCanonicalName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load provider for " + clazz.getCanonicalName(), e);
        }
    }

    protected Class loadClassFromResourceFile(String resourceName) throws Exception {
        URL url = this.loader.getResource(resourceName);
        if (url != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                return this.loader.loadClass(reader.readLine().trim());
            }
        }

        return null;
    }

    private Object instantiate(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getCanonicalName(), e);
        }
    }

    private ClassLoader loader;
    private static final Logger log = WunderBoss.logger(ClassPathLocator.class);
}
