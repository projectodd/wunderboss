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

package org.projectodd.wunderboss.as.wildfly;

import org.junit.Test;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.as.web.ServletWeb;
import org.projectodd.wunderboss.as.web.ServletWebProvider;
import org.projectodd.wunderboss.web.Web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServletWebTest {

    @Test
    public void testCanFindWebComponent() {
        WunderBoss.registerComponentProvider(Web.class, new ServletWebProvider(null, null, null));
        Web web = WunderBoss.findOrCreateComponent(Web.class);
        assertNotNull(web);
        assertTrue(web instanceof ServletWeb);
    }
}
