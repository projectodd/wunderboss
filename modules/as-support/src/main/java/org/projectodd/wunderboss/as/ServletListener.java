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

package org.projectodd.wunderboss.as;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.projectodd.wunderboss.ApplicationRunner;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.web.Web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

@WebListener
public class ServletListener implements ServletContextListener {
    public static final String TIMEOUT_PROPERTY = "wunderboss.deployment.timeout";
    public static final long DEFAULT_TIMEOUT_SECONDS = 240; // 4 minutes

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        WunderBoss.putOption("servlet-context-path", sc.getContextPath());

        /*
         We don't want to initialize the application on this thread, because if that hangs, we have
         no way to detect that or fail the deployment, so we run the init in a separate thread. But
         due to locks in jbossweb, we can't register servlets on a different thread. So we set up a
         queue that we pass along to ServletWeb, and it feeds servlet registration actions back to
         this thread. We also wrap the init with a Future, so we can know when it completes or time
         it out.

         We use the timeout as we poll the queue and wait for init to complete, and we also pass it
         to ServletWeb to use when it waits for the servlet actions to complete. It's a shared pool
         of time that both threads work down to prevent overrunning the timeout in toto.
        */
        final ActionConduit addServletActions = new ActionConduit();
        final CompletableFuture<Void> initDone = new CompletableFuture<>();
        long totalTimeout = DEFAULT_TIMEOUT_SECONDS;
        final String timeoutProp = System.getProperty(TIMEOUT_PROPERTY);

        if (timeoutProp != null) {
            totalTimeout = Long.parseLong(timeoutProp);
        }

        final AtomicLong sharedTimeout = new AtomicLong(totalTimeout * 1000);

        try {
            WunderBoss.registerComponentProvider(Web.class, new ServletWebProvider(sc, addServletActions, sharedTimeout));
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our web
        }

        this.applicationRunner = new ApplicationRunner(getName(sc)) {
            @Override
            protected void updateClassPath() throws Exception {
                super.updateClassPath();
                // TODO: Is this still needed? Things seem to work with it commented out
                ModuleUtils.addToModuleClasspath(Module.forClass(MSCService.class), classPathAdditions);
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

            @Override
            public void start(String[] args) {
                try {
                    super.start(args);
                    initDone.complete(null);
                } catch (Exception e) {
                    initDone.completeExceptionally(e);
                }
            }
        };

        WunderBoss.workerPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    applicationRunner.start(null);
                } catch (Exception ignored) {
                    // start() catches all exceptions, so won't really throw
                }
            }
        });

        while (sharedTimeout.get() > 0 &&
                !initDone.isDone()) {
            final Runnable action = addServletActions.poll();

            if (action != null) {
                action.run();
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for application initialization to complete", e);
                }

                sharedTimeout.addAndGet(-10);
            }
        }

        if (sharedTimeout.get() <= 0) {
            throw new RuntimeException(String.format("Timed out waiting for initialization to complete after %d " +
                                                             "seconds. If you need more time, use the %s sysprop.",
                                                     totalTimeout, TIMEOUT_PROPERTY));
        }

        // there is a potential race here, where ServletWeb can put an action on the queue after
        // we've exited the while() loop but before we've called close(), but we don't care, because
        // we'll only leave the while() if 1) the timeout has occurred (which will throw above),
        // or b) the init has signaled it is done (and therefore can't add an action)
        addServletActions.close();

        try {
            initDone.join();
        } catch (CompletionException e) {
            throw new RuntimeException("Application initialization failed", e.getCause());
        } catch (CancellationException e) {
            // shouldn't happen, but...
            throw new RuntimeException("Application initialization was cancelled", e);
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

    private String getName(ServletContext sc) {
        String result = sc.getServletContextName();
        if (result == null) {
            result = sc.getContextPath();
            if ("".equals(result)) {
                result = "/";
            }
        }
        
        return result;
    }

    private ApplicationRunner applicationRunner;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");
}
