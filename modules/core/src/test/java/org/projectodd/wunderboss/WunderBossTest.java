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
        WunderBoss.registerComponentProvider("test-register", new TestComponentProvider());
        assertTrue(WunderBoss.providesComponent("test-register"));
    }

    @Test
    public void testCachesComponents() {
        WunderBoss.registerComponentProvider("test-cache", new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent("test-cache");
        assertEquals("default", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent("test-cache"));
    }

    @Test
    public void testCachesComponentsWithNames() {
        WunderBoss.registerComponentProvider("test-cache2", new TestComponentProvider());
        Map options = new HashMap() {{put("name", "foobar");}};
        Component comp = WunderBoss.findOrCreateComponent("test-cache2", options);
        assertEquals("foobar", comp.name());
        assertEquals(comp, WunderBoss.findOrCreateComponent("test-cache2", options));
    }

    @Test
    public void testCanStopContainer() {
        WunderBoss.registerComponentProvider("test", new TestComponentProvider());
        Component comp = WunderBoss.findOrCreateComponent("test");
        WunderBoss.stop();
        assertTrue(((TestComponent)comp).stopped);
    }

    private TestLanguage testLanguage;
}
