package io.wunderboss.ruby;

import io.wunderboss.Language;
import io.wunderboss.Options;
import io.wunderboss.WunderBoss;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

public class RubyLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {

    }

    @Override
    public Ruby getRuntime(Options options) {
        String root = options.get("root", ".").toString();
        if (root.equals(".")) {
            return Ruby.getGlobalRuntime();
        } else {
            RubyInstanceConfig rubyConfig = new RubyInstanceConfig();
            rubyConfig.setLoadPaths(Arrays.asList(root));
            Ruby runtime = Ruby.newInstance(rubyConfig);
            runtime.setCurrentDirectory(root);
            return runtime;
        }
    }

    @Override
    public void destroyRuntime(Object runtime) {
        ((Ruby) runtime).tearDown(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        if (object instanceof IRubyObject) {
            return (T) ((IRubyObject) object).toJava(toClass);
        }
        return (T) object;
    }
}
