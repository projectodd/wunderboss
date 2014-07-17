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

public class ServletListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            WunderBoss.registerComponentProvider(Web.class, new WildflyWebProvider(sce.getServletContext()));
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our web
        }
        applicationRunner = new ApplicationRunner("foo") {
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

        try {
            applicationRunner.start(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    }

    private ApplicationRunner applicationRunner;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");
}
