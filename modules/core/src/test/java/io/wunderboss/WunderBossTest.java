package io.wunderboss;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WunderBossTest {

    @Before
    public void setUpContainer() {
        container = new WunderBoss();
        testLanguage = new TestLanguage();
        testComponent = new TestComponent();
    }

    @Test
    public void testCanRegisterLanguage() {
        container.registerLanguage("test", testLanguage);
        assertTrue(testLanguage.registered);
    }

    @Test
    public void testCanGetLanguageRegisteredLanguage() {
        container.registerLanguage("test", testLanguage);
        assertEquals(container.getLanguage("test"), testLanguage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCantGetUnknownLanguage() {
        container.getLanguage("test");
    }

    @Test
    public void testCanRegisterComponent() {
        container.registerComponent("test", testComponent);
        assertTrue(testComponent.registered);
        assertTrue(container.hasComponent("test"));
    }

    @Test
    public void testCanConfigureRegisteredComponent() {
        Map<String, Object> options = new HashMap<>();
        options.put("foo", "bar");
        container.registerComponent("test", testComponent);
        container.configure("test", options);
        assertEquals("bar", testComponent.configOptions.get("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCantConfigureUnknownComponent() {
        container.configure("test", new Options());
    }

    @Test
    public void testCanStopContainer() {
        container.registerComponent("test", testComponent);
        container.stop();
        assertTrue(testComponent.stopped);
    }

    private WunderBoss container;
    private TestLanguage testLanguage;
    private TestComponent testComponent;
}
