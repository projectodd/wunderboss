package io.wunderboss.ruby;

import io.wunderboss.Language;
import io.wunderboss.WunderBoss;
import org.jruby.Ruby;

public class RubyLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {

    }

    @Override
    public Ruby getRuntime() {
        return Ruby.getGlobalRuntime();
    }
}
