/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

public class TestComponent implements Component {

    public TestComponent(String name, Options opts) {
        this.name = name;
        this.configOptions = opts;
    }

    @Override
    public void start() {
        registered = true;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public boolean isRunning() {
        return registered && !stopped;
    }

    @Override
    public String name() {
        return this.name;
    }

    String name;
    boolean registered;
    Options configOptions;
    boolean stopped;
}
