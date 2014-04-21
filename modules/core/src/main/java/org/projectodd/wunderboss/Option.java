package org.projectodd.wunderboss;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Option {

    public String name;
    public Object defaultValue;

    public Option() {}

    public static <T extends Option> T opt(String name, Class T) {
        return opt(name, null, T);
    }

    public static <T extends Option> T opt(String name, Object defaultValue, Class T) {
        T opt = null;
        try {
            opt = (T)T.newInstance();
            opt.name = name;
            opt.defaultValue = defaultValue;
            rememberOption(opt);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return opt;
    }

    public static Set<Option> optsFor(Class clazz) {
        Set<Option> opts = new HashSet<>();
        while(Option.class.isAssignableFrom(clazz)) {
            if (knownOptions.containsKey(clazz)) {
                opts.addAll(knownOptions.get(clazz));
            }
            clazz = clazz.getSuperclass();
        }

        return opts;
    }

    private static void rememberOption(Option opt) {
        Set<Option> opts = knownOptions.get(opt.getClass());
        if (opts == null) {
            opts = new HashSet<>();
            knownOptions.put(opt.getClass(), opts);
        }
        opts.add(opt);
    }

    private static final Map<Class, Set<Option>> knownOptions = new HashMap<>();
}
