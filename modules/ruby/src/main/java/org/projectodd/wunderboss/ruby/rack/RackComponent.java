package org.projectodd.wunderboss.ruby.rack;

import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentInstance;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.ruby.RubyHelper;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;

public class RackComponent extends Component{
    @Override
    public String[] getLanguageDependencies() {
        return new String[]{"ruby"};
    }

    @Override
    public String[] getComponentDependencies() {
        return new String[]{"web"};
    }

    @Override
    public void boot() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void configure(Options options) {
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context", "/");
        String root = options.getString("root", ".");
        String rackup = options.getString("rackup", "config.ru");
        String staticDirectory = options.getString("static_dir", root + "/public");
        Object rackApp = options.get("rack_app");

        if (!new File(rackup).isAbsolute()) {
            rackup = root + File.separator + rackup;
        }

        IRubyObject rackApplication;
        if (rackApp != null) {
            rackApplication = (IRubyObject) rackApp;
        } else {
            String rackScript = "require 'rack'\n" +
                    "app, _ = Rack::Builder.parse_file('" + rackup + "')\n" +
                    "app\n";
            rackApplication = RubyHelper.evalScriptlet(getRuntime(application), rackScript, false);
        }

        try {
            RackHandler rackHandler = new RackHandler(rackApplication, context);

            Options webOptions = new Options();
            webOptions.put("context", context);
            webOptions.put("http_handler", rackHandler);
            webOptions.put("static_dir", staticDirectory);
            ComponentInstance web = application.start("web", webOptions);

            Options instanceOptions = new Options();
            instanceOptions.put("web", web);
            return new ComponentInstance(this, instanceOptions);
        } catch (Exception e) {
            // TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance web = (ComponentInstance) instance.getOptions().get("web");
        web.stop();
    }

    private Ruby getRuntime(Application application) {
        return (Ruby) application.runtime();
    }
}
