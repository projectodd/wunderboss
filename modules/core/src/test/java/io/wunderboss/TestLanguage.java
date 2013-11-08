package io.wunderboss;

import io.wunderboss.Language;

public class TestLanguage implements Language {
    @Override
    public void initialize(WunderBoss container) {
        registered = true;
    }

    @Override
    public Object getRuntime() {
        return null;
    }

    boolean registered;
}
