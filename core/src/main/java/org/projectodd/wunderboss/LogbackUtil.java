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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;

import java.net.URL;

public class LogbackUtil {

    public static void setLogLevel(Logger logger, String level) {
        ((ch.qos.logback.classic.Logger)logger)
                .setLevel(Level.toLevel(level));
    }

    public static void configureLogback(Object rawContext) {
        if (rawContext instanceof LoggerContext) {
            LoggerContext context = (LoggerContext)rawContext;
            ContextInitializer initializer = new ContextInitializer(context);
            if (initializer.findURLOfDefaultConfigurationFile(false) == null) {
                context.reset();
                URL defaultConfig = context.getClass().getClassLoader().getResource("logback-default.xml");
                try {
                    initializer.configureByResource(defaultConfig);
                } catch (JoranException e) {
                    throw new RuntimeException("Failed to load default logging configuration", e);
                }
            }
        }
    }
}
