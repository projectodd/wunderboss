package io.wunderboss;

import io.wunderboss.web.Wundertow;

import java.util.Map;

public abstract class Application {

    protected Application(WunderBoss wunderBoss, Map<String, String> config) {
        this.wunderBoss = wunderBoss;
    }

    protected Wundertow getWundertow() {
        return this.wunderBoss.getWundertow();
    }

    protected WunderBoss getWunderBoss() {
        return this.wunderBoss;
    }

    private WunderBoss wunderBoss;

    public abstract void deployWeb(Map<String, String> config) throws Exception;
}
