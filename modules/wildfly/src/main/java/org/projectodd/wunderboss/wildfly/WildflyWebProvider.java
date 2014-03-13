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

package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.Web;
import org.wildfly.extension.undertow.UndertowService;

public class WildflyWebProvider implements ComponentProvider<Web> {

    public WildflyWebProvider(UndertowService service) {
        this.service = service;

    }
    @Override
    public Web create(String name, Options ignored) {
        return new WildFlyWeb(name, this.service);
    }

    private final UndertowService service;
}
