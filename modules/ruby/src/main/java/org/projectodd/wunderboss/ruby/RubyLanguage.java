package org.projectodd.wunderboss.ruby;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.util.Arrays;

public class RubyLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {
        this.container = container;
    }

    @Override
    public synchronized Ruby runtime() {
        if (this.runtime == null) {
            String root = this.container.options().get("root", ".").toString();
            if (Ruby.isGlobalRuntimeReady()) {
                this.runtime = Ruby.getGlobalRuntime();
            } else {
                RubyInstanceConfig rubyConfig = new RubyInstanceConfig();
                rubyConfig.setLoadPaths(Arrays.asList(root));
                this.runtime = Ruby.newInstance(rubyConfig);
                this.createdRuntime = true;
            }
            String expandedRoot = this.runtime.evalScriptlet("File.expand_path(%q(" + root + "))").asJavaString();
            this.runtime.setCurrentDirectory(expandedRoot);
        }

        return this.runtime;
    }

    @Override
    public synchronized void shutdown() {
        if (this.runtime != null && this.createdRuntime) {
            this.runtime.tearDown(false);
            this.runtime = null;
        }
    }

    @Override
    public Object eval(String toEval) {
        return runtime().evalScriptlet(toEval);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        if (object instanceof IRubyObject) {
            return (T) ((IRubyObject) object).toJava(toClass);
        }
        return (T) object;
    }

    private WunderBoss container;
    private Ruby runtime;
    private boolean createdRuntime = false;
}
