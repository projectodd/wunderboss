package io.wunderboss;

public interface Language {

    public void initialize(WunderBoss container);

    public Object getRuntime(Options options);

    public void destroyRuntime(Object runtime);
}
