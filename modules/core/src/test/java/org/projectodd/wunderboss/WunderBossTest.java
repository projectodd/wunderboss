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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WunderBossTest {

    @Before
    public void setUpContainer() {
        testLanguage = new TestLanguage();
    }

    @Test
    public void testCanRegisterLanguage() {
        WunderBoss.registerLanguage("test", testLanguage);
        assertTrue(testLanguage.registered);
    }

    @Test
    public void testCanGetLanguageRegisteredLanguage() {
        WunderBoss.registerLanguage("test", testLanguage);
        assertEquals(WunderBoss.findLanguage("test"), testLanguage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCantGetUnknownLanguage() {
        WunderBoss.findLanguage("not-found");
    }

    @Test
    public void testCanRegisterComponent() {
        WunderBoss.registerComponentProvider(TestComponent.class, new TestComponentProvider());
        assertTrue(WunderBoss.providesComponent(TestComponent.class));
    }

    @Test
    public void testLastRegisteredComponentWins() {
        final TestComponent myComponent = new TestComponent("mine", null);
        ComponentProvider<TestComponent> myProvider = new ComponentProvider<TestComponent>() {
            @Override
            public TestComponent create(String name, Options options) {
                return myComponent;
            }
        };
        WunderBoss.registerComponentProvider(TestComponent.class, new TestComponentProvider());
        WunderBoss.registerComponentProvider(TestComponent.class, myProvider);
        TestComponent testComponent = WunderBoss.findOrCreateComponent(TestComponent.class, "uniquename123", null);
        assertEquals(testComponent, myComponent);
    }

    @Test
    public void testCachesComponents() {
        WunderBoss.registerComponentProvider(TestComponent.class, new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent(TestComponent.class);
        assertEquals("default", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent(TestComponent.class));
        WunderBoss.findOrCreateComponent(TestComponent.class);
    }

    @Test
    public void testCachesComponentsWithNames() {
        WunderBoss.registerComponentProvider(TestComponent.class, new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent(TestComponent.class, "foobar", null);
        assertEquals("foobar", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent(TestComponent.class, "foobar", null));
    }

    @Test
    public void testCanStopContainer() throws Exception {
        WunderBoss.registerComponentProvider(TestComponent.class, new TestComponentProvider());
        TestComponent comp = WunderBoss.findOrCreateComponent(TestComponent.class);
        WunderBoss.shutdownAndReset();
        assertTrue(comp.stopped);
    }

    private TestLanguage testLanguage;
}
