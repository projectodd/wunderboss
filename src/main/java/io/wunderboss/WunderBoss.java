package io.wunderboss;

import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.wunderboss.rack.RackServlet;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class WunderBoss {

    public WunderBoss(Map<String, String> config) {
        this.wundertow = new Wundertow(config);
    }

    public void deployRubyApplication(String applicationRoot, Map<String, String> config) throws Exception {
        Ruby ruby = null;
        if (applicationRoot.equals(".")) {
            ruby = Ruby.getGlobalRuntime();
        } else {
            ruby = Ruby.newInstance();
            ruby.setCurrentDirectory(applicationRoot);
        }

        if (config.get("web_context") != null) {
            this.wundertow.deployRackApplication(applicationRoot, ruby, config);
        }
    }
    private Wundertow wundertow;

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}
