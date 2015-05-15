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

package org.projectodd.wunderboss.wildfly;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.projectodd.wunderboss.ApplicationRunner;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.web.Web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServletListener implements ServletContextListener {
    public static final String TIMEOUT_PROPERTY = "wunderboss.deployment.timeout";
    public static final long DEFAULT_TIMEOUT = 240; // 4 minutes

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WunderBoss.putOption("servlet-context-path", sce.getServletContext().getContextPath());
        try {
            WunderBoss.registerComponentProvider(Web.class, new WildflyWebProvider(sce.getServletContext()));
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our web
        }
        applicationRunner = new ApplicationRunner(getName(sce)) {
            @Override
            protected void updateClassPath() throws Exception {
                super.updateClassPath();
                // TODO: Is this still needed? Things seem to work with it commented out
                ModuleUtils.addToModuleClasspath(Module.forClass(WildFlyService.class), classPathAdditions);
            }
            @Override
            protected URL jarURL() {
                String mainPath = ApplicationRunner.class.getName().replace(".", "/") + ".class";
                String mainUrl = ApplicationRunner.class.getClassLoader().getResource(mainPath).toString();
                String marker = ".jar";
                int to = mainUrl.lastIndexOf(marker);
                try {
                    return new URL(mainUrl.substring(0, to + marker.length()));
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Error determining jar url", e);
                }
            }
        };

        Future<?> future = WunderBoss.workerPool().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                applicationRunner.start(null);

                return null;
            }
        });

        long timeout = DEFAULT_TIMEOUT;
        String timeoutProp = System.getProperty(TIMEOUT_PROPERTY);
        if (timeoutProp != null) {
            timeout = Long.parseLong(timeoutProp);
        }

        try {
            future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException _) {
            future.cancel(true);
            String message = String.format("Timed out waiting for initialization to complete after %d seconds. If you need more time, use the %s sysprop.",
                                           timeout, TIMEOUT_PROPERTY);
            log.error(message);
            throw new RuntimeException(message);
        } catch (InterruptedException _) {
            future.cancel(true);
            throw new RuntimeException("Initialization failed");
        } catch (ExecutionException e) {
            future.cancel(true);
            throw new RuntimeException("Initialization failed", e.getCause());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("Stopping WunderBoss application");
        try {
            WunderBoss.shutdownAndReset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (applicationRunner != null) {
                applicationRunner.stop();
                applicationRunner = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
         Clear any drivers the app registered to prevent permgen leaks
         getDrivers will only return drivers we have the right to see, so
         this shouldn't affect drivers registered by other apps.
         see IMMUTANT-417
         */
        try {
            for(Driver driver : Collections.list(DriverManager.getDrivers())) {
                DriverManager.deregisterDriver(driver);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getName(ServletContextEvent sce) {
        String result = sce.getServletContext().getServletContextName();
        if (result == null) {
            result = sce.getServletContext().getContextPath();
            if ("".equals(result)) {
                result = "/";
            }
        }
        
        return result;
    }

    private ApplicationRunner applicationRunner;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");
}
