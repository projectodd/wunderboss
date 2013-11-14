package org.projectodd.wunderboss;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {

    @Before
    public void setUpContainer() {
        container = new WunderBoss();
        container.registerLanguage("test", new TestLanguage());
        application = container.newApplication("test");
        testComponent = new TestComponent();
    }

    @Test
    public void testCanStartComponent() {
        container.registerComponent("test", testComponent);
        application.start("test");
        assertTrue(testComponent.started);
    }

    @Test
    public void testCanStartComponentWithOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("foo", "bar");
        container.registerComponent("test", testComponent);
        application.start("test", options);
        assertEquals("bar", testComponent.startOptions.get("foo"));
    }

    @Test
    public void testCanStopApplication() {
        container.registerComponent("test", testComponent);
        ComponentInstance instance = application.start("test");
        application.stop();
        assertTrue(instance.isStopped());
    }

    private WunderBoss container;
    private Application application;
    private TestComponent testComponent;
}
