package io.wunderboss;

import io.wunderboss.web.Wundertow;

import java.util.ArrayList;
import java.util.List;
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

    public void deployWeb(String context, Map<String, String> config) throws Exception {
        webContexts.add(context);
    }

    public void undeploy() throws Exception {
        for (String context : webContexts) {
            getWundertow().undeploy(context);
        }
    }

    private List<String> webContexts = new ArrayList<>();
}
