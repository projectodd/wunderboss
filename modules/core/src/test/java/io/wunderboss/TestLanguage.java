package io.wunderboss;

import io.wunderboss.Language;

public class TestLanguage implements Language {
    @Override
    public void initialize(WunderBoss container) {
        registered = true;
    }

    @Override
    public String getRuntime(Options options) {
        return "runtime";
    }

    @Override
    public void destroyRuntime(Object runtime) {
        destroyed = true;
    }

    @Override
    public Options transformOptions(Options options) {
        return options;
    }

    boolean registered;
    boolean destroyed;
}
