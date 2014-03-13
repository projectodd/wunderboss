package org.projectodd.wunderboss.web;

import org.junit.Test;
import org.projectodd.wunderboss.WunderBoss;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UndertowWebTest {

    @Test
    public void testCanFindWebComponent() {
        Web web = WunderBoss.findOrCreateComponent(Web.class);
        assertNotNull(web);
        assertTrue(web instanceof UndertowWeb);
    }
}
