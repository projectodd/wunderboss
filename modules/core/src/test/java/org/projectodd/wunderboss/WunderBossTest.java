package org.projectodd.wunderboss;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
        WunderBoss.registerComponentProvider(new TestComponentProvider());
        assertTrue(WunderBoss.providesComponent(TestComponent.class));
    }

    @Test
    public void testCachesComponents() {
        WunderBoss.registerComponentProvider(new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent(TestComponent.class);
        assertEquals("default", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent(TestComponent.class));
        WunderBoss.findOrCreateComponent(TestComponent.class);
    }

    @Test
    public void testCachesComponentsWithNames() {
        WunderBoss.registerComponentProvider(new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent(TestComponent.class, "foobar", null);
        assertEquals("foobar", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent(TestComponent.class, "foobar", null));
    }

    @Test
    public void testCanStopContainer() throws Exception {
        WunderBoss.registerComponentProvider(new TestComponentProvider());
        TestComponent comp = WunderBoss.findOrCreateComponent(TestComponent.class);
        WunderBoss.stop();
        assertTrue(comp.stopped);
    }

    private TestLanguage testLanguage;
}
