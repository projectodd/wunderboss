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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.projectodd.wunderboss.WunderBoss;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ASUtils {

    private static final ServiceName[] JGROUPS_FACTORY_NAMES =
            { ServiceName.parse("jboss.jgroups.stack"),                 // WF8, EAP
              ServiceName.parse("jboss.jgroups.factory.default-stack"), // WF9
              ServiceName.parse("jboss.jgroups.factory.default") };     // WF10
    private static Boolean inCluster = null;

    public static boolean inCluster() {
        if (inCluster == null) {
            //look for a jgroups stack to see if we are clustered
            ServiceRegistry registry = (ServiceRegistry) WunderBoss.options().get("service-registry");
            if (registry != null) {
                for (ServiceName each : JGROUPS_FACTORY_NAMES) {
                    if (registry.getService(each) != null) {
                        inCluster = true;
                        break;
                    }
                }
            }

            if (inCluster == null) {
                inCluster = false;
            }
        }

        return inCluster;
    }

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

    public static boolean containerIsWildFly10() {
        return CONTAINER_IS_WILDFLY_10;
    }

    public static boolean containerIsEAP() {
        return containerType() == ContainerType.EAP;
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
                    containerIsWildFly9() ||
                    containerIsWildFly10();

            if (!asyncSupported) {
                log.warn("NOTE: HTTP stream sends are synchronous in WildFly " + CONTAINER_VERSION +
                                 ". Use 9.0.0.Alpha1 or higher to have asynchronous sends.");
            }
        }

        return asyncSupported;
    }

    private static final String WF10_MESSAGING_PREFIX = "org.wildfly.extension.messaging.activemq.";
    private static final String NOT_WF10_MESSAGING_PREFIX = "org.jboss.as.messaging.";

    public static ServiceName messagingServiceName() {
        String prefix;
        String methodName;
        try {
            if (containerIsWildFly10()) {
                prefix = WF10_MESSAGING_PREFIX;
                methodName = "getActiveMQServiceName";
            } else {
                prefix = NOT_WF10_MESSAGING_PREFIX;
                methodName = "getHornetQServiceName";
            }

            Method method = loadClass(prefix + "MessagingServices").getMethod(methodName, String.class);

            return (ServiceName) method.invoke(null, "default");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to determine messaging service name", e);
        }
    }

    public static ServiceName queueServiceName(String name) {
        return ((ServiceName) callJMSServicesMethod("getJmsQueueBaseServiceName",
                                                    messagingServiceName()))
                .append(name);
    }

    public static ServiceName topicServiceName(String name) {
        return ((ServiceName) callJMSServicesMethod("getJmsTopicBaseServiceName",
                                                    messagingServiceName()))
                .append(name);
    }

    private static Object callJMSServicesMethod(String methodName, Object... args) {
        try  {
            Class clazz = loadClass((containerIsWildFly10() ? WF10_MESSAGING_PREFIX : NOT_WF10_MESSAGING_PREFIX)
                                        + "jms.JMSServices");
            Method method = null;
            for(Method each : clazz.getMethods()) {
                if (methodName.equals(each.getName())) {
                    method = each;
                    break;
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("No method " + methodName + " on class " + clazz);
            }

            return method.invoke(null, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke JMSSservices method", e);
        }
    }

    public static Class queueServiceClass() {
        return loadClass((containerIsWildFly10() ? WF10_MESSAGING_PREFIX : NOT_WF10_MESSAGING_PREFIX) +
                                 "jms.JMSQueueService");
    }

    public static Class topicServiceClass() {
        return loadClass((containerIsWildFly10() ? WF10_MESSAGING_PREFIX : NOT_WF10_MESSAGING_PREFIX) +
                                 "jms.JMSTopicService");
    }

    private static Class loadClass(String name) {
        try {
            return ASUtils.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class " + name, e);
        }
    }

    // this can't be recursive, as it can blow the stack
    public static Object waitForAppearanceInJNDI(final Context ctx, final String jndiName, long timeout) {
        while (timeout > 0) {
            try {
                Object result = ctx.lookup(jndiName);
                if (result != null) {
                    return result;
                }
            } catch (NameNotFoundException ignored) {
            } catch (NamingException ex) {
                throw new RuntimeException("Failed JNDI lookup for " + jndiName, ex);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex2) {
                throw new RuntimeException("Interrupted while doing JNDI lookup for " + jndiName);
            }

            timeout -= 10;
        }

        return null;
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
        final String swarmVersion = System.getProperty("wildfly.swarm.version");
        if (swarmVersion != null) {
            CONTAINER_TYPE = ContainerType.WILDFLY;
            CONTAINER_IS_WILDFLY_9 = false;
            CONTAINER_IS_WILDFLY_10 = true;
            CONTAINER_VERSION = "unknown";
        } else {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            String version = null;
            String productName = null;
            try {
                ObjectName name = new ObjectName("jboss.as:management-root=server");

                // EAP & 9.x stores it under "productVersion"
                version = (String) mbs.getAttribute(name, "productVersion");
                if (version == null) {
                    // 8.x stores it under "releaseVersion"
                    version = (String) mbs.getAttribute(name, "releaseVersion");
                }

                productName = (String) mbs.getAttribute(name, "productName");

            } catch (OperationsException |
                    MBeanException |
                    ReflectionException ignored) {
            }

            CONTAINER_VERSION = version;

            // WF 8 doesn't set the productName, so we can't identify solely based on it

            ContainerType type = ContainerType.UNKNOWN;
            if ("EAP".equals(productName)) {
                type = ContainerType.EAP;
            } else if (version != null &&
                    version.startsWith("8.")) {
                type = ContainerType.WILDFLY;
            } else if (productName != null &&
                    productName.startsWith("WildFly")) {
                // WF 9 (and up) actually set the productName
                type = ContainerType.WILDFLY;
            }

            CONTAINER_TYPE = type;
            CONTAINER_IS_WILDFLY_9 = type == ContainerType.WILDFLY &&
                    version != null &&
                    version.startsWith("9.");
            CONTAINER_IS_WILDFLY_10 = type == ContainerType.WILDFLY &&
                    version != null &&
                    version.startsWith("10.");
        }
    }

    private static final String CONTAINER_VERSION;
    private static final ContainerType CONTAINER_TYPE;
    private static final boolean CONTAINER_IS_WILDFLY_9;
    private static final boolean CONTAINER_IS_WILDFLY_10;
    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.as");
    private static Boolean asyncSupported;

}
