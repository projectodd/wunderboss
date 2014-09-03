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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;

public class LogbackUtil {

    public static void setLogLevel(Logger logger, String level) {
        ((ch.qos.logback.classic.Logger)logger)
                .setLevel(Level.toLevel(level));
    }

    public static void configureLogback(Object rawContext) {
        if (!(rawContext instanceof LoggerContext)) {
            return;
        }

        LoggerContext context = (LoggerContext)rawContext;
        ClassLoader cl = LogbackUtil.class.getClassLoader();
        //TODO: see if there is a better way to determine if logback will self-configure
        if (System.getProperty("logback.configurationFile") == null &&
                cl.getResource("logback.xml") == null &&
                cl.getResource("logback.groovy") == null) {
            context.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            try {
                configurator.doConfigure(cl.getResource("logback-default.xml"));
            } catch (JoranException e) {
                throw new RuntimeException("Failed to load default logging configuration", e);
            }
        }
    }
}
