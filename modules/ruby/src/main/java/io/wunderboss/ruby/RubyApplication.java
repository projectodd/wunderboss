package io.wunderboss.ruby;

import io.wunderboss.Application;
import io.wunderboss.WunderBoss;
import io.wunderboss.ruby.rack.RackServlet;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RubyApplication extends Application {

    public RubyApplication(WunderBoss wunderBoss, Map<String, String> config) {
        super(wunderBoss, config);
        root = config.get("root");
        if (root.equals(".") && Ruby.getGlobalRuntime() != null) {
            // We're running embedded and the user is deploying the current app
            runtime = Ruby.getGlobalRuntime();
        } else {
            RubyInstanceConfig rubyConfig = new RubyInstanceConfig();
            rubyConfig.setLoadPaths(Arrays.asList(root));
            runtime = Ruby.newInstance(rubyConfig);
            runtime.setCurrentDirectory(root);
        }
    }

    @Override
    public void deployWeb(String context, Map<String, String> config) throws Exception {
        super.deployWeb(context, config);
        if (!config.containsKey("static_dir")) {
            config.put("static_dir", root + "/public");
        }
        StringBuilder rackScript = new StringBuilder()
                .append("require 'rack'\n")
                .append("app, _ = Rack::Builder.parse_file(File.join('" + root + "', 'config.ru'))\n")
                .append("app\n");
        IRubyObject rackApplication = RuntimeHelper.evalScriptlet(runtime, rackScript.toString(), false);
        Map<String, Object> servletContextAttributes = new HashMap<>();
        servletContextAttributes.put("rack_application", rackApplication);
        getWundertow().deployServlet(context, RackServlet.class, servletContextAttributes, config);
    }

    @Override
    public void undeploy() throws Exception {
        try {
            super.undeploy();
        }
        finally {
            if (runtime != null) {
                runtime.tearDown(false);
                runtime = null;
            }
        }
    }

    private Ruby runtime;
    private String root;
}
