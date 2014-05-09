/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
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

public class TestLanguage implements Language {
    @Override
    public void initialize() {
        registered = true;
    }

    @Override
    public String runtime() {
        return "runtime";
    }

    @Override
    public void shutdown() {
        destroyed = true;
    }

    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    @Override
    public Object eval(String toEval) {
        return null;
    }

    boolean registered;
    boolean destroyed;
}
