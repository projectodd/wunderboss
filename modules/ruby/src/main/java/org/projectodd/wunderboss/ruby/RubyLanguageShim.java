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

package org.projectodd.wunderboss.ruby;

import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;

/**
 * A wrapper around RubyLanguage to make sure we get JRuby on the
 * classpath before linking against any of its classes
 */
public class RubyLanguageShim implements Language {

    @Override
    public void initialize() {
    }

    @Override
    public synchronized Object runtime() {
        return ruby().runtime();
    }

    @Override
    public synchronized void shutdown() {
        ruby().shutdown();
    }

    @Override
    public Object eval(String toEval) {
        return ruby().eval(toEval);
    }

    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return ruby().coerceToClass(object, toClass);
    }

    protected String extractedRoot() {
        return WunderBoss.options().getString("extract-root", ".");
    }

    protected Language ruby() {
        if (rubyLanguage == null) {
            try {
                String jrubyHome = locateJRubyHome();
                File jrubyLib = new File(jrubyHome + "/lib");
                if (jrubyLib.isDirectory()) {
                    WunderBoss.putOption("jruby-home", jrubyHome);
                    for (File each : jrubyLib.listFiles(JAR_FILTER)) {
                        WunderBoss.updateClassPath(each.toURI().toURL());
                    }
                }

                checkForJRuby();

                Class<?> rubyLanguageClass = Class.forName("org.projectodd.wunderboss.ruby.RubyLanguage",
                        true, WunderBoss.classLoader());
                rubyLanguage = (Language) rubyLanguageClass.newInstance();
                rubyLanguage.initialize();
            } catch (ClassNotFoundException |
                    InstantiationException |
                    IllegalAccessException |
                    MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return rubyLanguage;
    }

    protected void checkForJRuby() {
        try {
            Class.forName("org.jruby.Ruby", false, WunderBoss.classLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No JRuby found - either include JRuby in the artifact, " +
                    "set the JRUBY_HOME envvar, or set the jruby.home sysprop.");
        }
    }

    protected String locateJRubyHome() {
        String home = System.getProperty("jruby.home");

        if (home == null) {
            home = System.getenv("JRUBY_HOME");
        }

        if (home == null) {
            String root = extractedRoot();
            if ((new File(root + "/jruby/lib")).isDirectory()) {
                home = root + "/jruby";
            }
        }

        return home;
    }

    private Language rubyLanguage;
    static final FileFilter JAR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.getName().endsWith(".jar");
        }
    };
}
