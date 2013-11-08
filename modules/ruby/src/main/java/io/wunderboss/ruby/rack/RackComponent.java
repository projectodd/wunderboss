package io.wunderboss.ruby.rack;

import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import io.wunderboss.ruby.RuntimeHelper;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

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
    public ComponentInstance start(Options options) {
        String context = options.get("context", "/").toString();
        String root = options.get("root", ".").toString();
        String staticDirectory = options.get("static_dir", root + "/public").toString();

        StringBuilder rackScript = new StringBuilder()
                .append("require 'rack'\n")
                .append("app, _ = Rack::Builder.parse_file(File.join('" + root + "', 'config.ru'))\n")
                .append("app\n");
        IRubyObject rackApplication = RuntimeHelper.evalScriptlet(getRuntime(), rackScript.toString(), false);

        Map<String, Object> servletContextAttributes = new HashMap<>();
        servletContextAttributes.put("rack_application", rackApplication);

        Options servletOptions = new Options();
        servletOptions.put("context", context);
        servletOptions.put("static_dir", staticDirectory);
        servletOptions.put("servlet_class", RackServlet.class);
        servletOptions.put("context_attributes", servletContextAttributes);
        ComponentInstance servlet = getContainer().start("servlet", servletOptions);

        Options instanceOptions = new Options();
        instanceOptions.put("servlet", servlet);
        ComponentInstance instance = new ComponentInstance(this, instanceOptions);

        return instance;
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance servlet = (ComponentInstance) instance.getOptions().get("servlet");
        servlet.stop();
    }

    private Ruby getRuntime() {
        return (Ruby) getContainer().getLanguage("ruby").getRuntime();
    }
}
