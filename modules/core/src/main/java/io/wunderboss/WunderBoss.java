package io.wunderboss;

import io.wunderboss.web.Wundertow;
import org.jboss.logging.Logger;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class WunderBoss {

    public WunderBoss(Map<String, String> config) {
        this.wundertow = new Wundertow(config);
    }

    public Application deployApplication(Map<String, String> config) throws Exception {
        String language = config.get("language");
        Constructor<Application> constructor = languages.get(language).getConstructor(
                WunderBoss.class, Map.class);
        return constructor.newInstance(this, config);
    }

    public static void registerLanguage(String language, String applicationClassName) throws Exception {
        Class<Application> klass = (Class<Application>) Class.forName(applicationClassName);
        languages.put(language, klass);
    }

    public Wundertow getWundertow() {
        return this.wundertow;
    }

    private Wundertow wundertow;

    private static Map<String, Class<Application>> languages = new HashMap<>();
    private static final Logger log = Logger.getLogger(WunderBoss.class);
}
