/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.messaging.jms;

import java.util.concurrent.Callable;

public class DestinationUtil {

    public static String jndiName(String name, JMSDestination.Type type) {
        return ("java:/jms/" + type.name + '/' + name).replace("//", "/_/");
    }

    public static boolean isJndiName(String name) {
        return name.startsWith("java:");
    }

    public static String fullName(String name, JMSDestination.Type type) {
        if (isJndiName(name)) {
            return name;
        } else {
            return jmsName(name, type);
        }
    }

    public static String jmsName(String name, JMSDestination.Type type) {
        return "jms." + type.name + "." + name;
    }

    public static Object mightThrow(Callable c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
