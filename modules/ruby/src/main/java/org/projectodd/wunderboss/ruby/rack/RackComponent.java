package org.projectodd.wunderboss.ruby.rack;

import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentInstance;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.ruby.RuntimeHelper;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RackComponent extends Component{
    @Override
    public String[] getLanguageDependencies() {
        return new String[]{"ruby"};
    }

    @Override
    public String[] getComponentDependencies() {
        return new String[]{"servlet"};
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
            rackApplication = RuntimeHelper.evalScriptlet(getRuntime(application), rackScript, false);
        }

        Map<String, Object> servletContextAttributes = new HashMap<>();
        servletContextAttributes.put("rack_application", rackApplication);

        Options servletOptions = new Options();
        servletOptions.put("context", context);
        servletOptions.put("static_dir", staticDirectory);
        servletOptions.put("servlet_class", RackServlet.class);
        servletOptions.put("context_attributes", servletContextAttributes);
        ComponentInstance servlet = application.start("servlet", servletOptions);

        Options instanceOptions = new Options();
        instanceOptions.put("servlet", servlet);
        return new ComponentInstance(this, instanceOptions);
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance servlet = (ComponentInstance) instance.getOptions().get("servlet");
        servlet.stop();
    }

    private Ruby getRuntime(Application application) {
        return (Ruby) application.getRuntime();
    }
}
