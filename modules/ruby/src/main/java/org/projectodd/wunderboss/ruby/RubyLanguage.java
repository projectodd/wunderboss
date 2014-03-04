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
            this.runtime = createRuntime(root);
            String expandedRoot = this.runtime.evalScriptlet("File.expand_path(%q(" + root + "))").asJavaString();
            this.runtime.setCurrentDirectory(expandedRoot);
        }

        return this.runtime;
    }

    protected Ruby createRuntime(String root) {
        Ruby runtime = Ruby.getGlobalRuntime();
        runtime.getLoadService().addPaths(root);
        return runtime;
    }

    @Override
    public synchronized void shutdown() {
        if (this.runtime != null && this.runtime != Ruby.getGlobalRuntime()) {
            this.runtime.tearDown(false);
        }
        this.runtime = null;
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
}