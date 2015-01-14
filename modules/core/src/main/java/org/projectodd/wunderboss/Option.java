/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
