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

package org.projectodd.wunderboss.as;

import org.jboss.logging.Logger;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.lang.management.ManagementFactory;

public class ASUtils {

    public enum ContainerType {
        EAP("EAP"), WILDFLY("WildFly"), UNKNOWN("unknown");

        public String name;

        ContainerType(String n) {
            this.name = n;
        }
    }

    public static boolean containerIsWildFly9() {
        return CONTAINER_IS_WILDFLY_9;
    }

    public static boolean containerIsWildFly8() {
        return containerType()==ContainerType.WILDFLY && !containerIsWildFly9();
    }

    public static ContainerType containerType() {
        return CONTAINER_TYPE;
    }

    public static String containerTypeAsString() {
        return CONTAINER_TYPE.name;
    }

    public static String containerVersion() {
        return CONTAINER_VERSION;
    }

    public static boolean isAsyncStreamingSupported() {
        if (asyncSupported == null) {
            asyncSupported = containerType() == ContainerType.EAP ||
                    containerIsWildFly9();

            if (!asyncSupported) {
                log.warn("NOTE: HTTP stream sends are synchronous in WildFly " + CONTAINER_VERSION +
                                 ". Use 9.0.0.Alpha1 or higher to have asynchronous sends.");
            }
        }

        return asyncSupported;
    }

    // this can't be recursive, as it can blow the stack
    public static Object waitForAppearanceInJNDI(final Context ctx, final String jndiName, long timeout) {
        Object result = null;
        boolean found = false;
        while (!found && timeout > 0) {
            try {
                result = ctx.lookup(jndiName);
                found = true;
            } catch (NameNotFoundException ex) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex2) {
                    throw new RuntimeException("Interrupted while doing JNDI lookup for " + jndiName);
                }

                timeout -= 10;
            } catch (NamingException ex) {
                throw new RuntimeException("Failed JNDI lookup for " + jndiName, ex);
            }
        }

        return result;
    }

    // this can't be recursive, as it can blow the stack
    public static boolean waitForRemovalFromJNDI(final Context ctx, final String jndiName, long timeout) {
        boolean removed = false;
        while (!removed && timeout > 0) {
            try {
                ctx.lookup(jndiName);
            } catch (NameNotFoundException ex) {
                removed = true;
            } catch (NamingException ex) {
                throw new RuntimeException("Failed JNDI lookup for " + jndiName, ex);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interrupted while doing JNDI lookup for " + jndiName);
            }

            timeout -= 10;
        }

        return removed;
    }

    static {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String version = null;
        String productName = null;
        try {
            ObjectName name = new ObjectName("jboss.as:management-root=server");

            // EAP & 9.x stores it under "productVersion"
            version = (String)mbs.getAttribute(name, "productVersion");
            if (version == null) {
                // 8.x stores it under "releaseVersion"
                version = (String)mbs.getAttribute(name, "releaseVersion");
            }

            productName = (String) mbs.getAttribute(name, "productName");

        } catch (OperationsException |
                MBeanException |
                ReflectionException ignored) {}

        CONTAINER_VERSION = version;

        // WF 8 doesn't set the productName, so we can't identify solely based on it

        ContainerType type = ContainerType.UNKNOWN;
        boolean wf9 = false;
        if ("EAP".equals(productName)) {
            type = ContainerType.EAP;
        } else if (version != null &&
                version.startsWith("8.")) {
            type = ContainerType.WILDFLY;
        } else if (productName != null &&
                productName.startsWith("WildFly")) {
            // WF 9 (and up) actually set the productName
            type = ContainerType.WILDFLY;
            wf9 = true;
        }

        CONTAINER_TYPE = type;
        CONTAINER_IS_WILDFLY_9 = wf9;
    }
    private static final String CONTAINER_VERSION;
    private static final ContainerType CONTAINER_TYPE;
    private static final boolean CONTAINER_IS_WILDFLY_9;
    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.as");
    private static Boolean asyncSupported;

}
