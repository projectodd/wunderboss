package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ruby.RubyLanguage;
import org.projectodd.wunderboss.ruby.rack.RackComponent;

public class WildFlyBootstrapper {

    public WildFlyBootstrapper() {
//        Options options = new Options();
//        options.put("root", "/home/bbrowning/tmp/blah2");
//        container = new WunderBoss(options);
//        container.registerLanguage("ruby", new WildFlyRubyLanguage());
//        container.registerComponent("web", new WildFlyWebComponent());
//        container.registerComponent("rack", new RackComponent());
    }

    public void start() {
//        System.err.println("!!! Starting Ruby application");
//        application = container.newApplication("ruby");
//        application.start("rack");
    }

    public void stop() {
//        if (application != null) {
//            System.err.println("!!! Stopping Ruby application");
//            application.stop();
//        }
//        container.stop();
    }

    private WunderBoss container;
    private Application application;
}
