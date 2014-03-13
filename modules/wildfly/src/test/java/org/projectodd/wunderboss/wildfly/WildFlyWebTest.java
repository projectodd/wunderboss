package org.projectodd.wunderboss.wildfly;

import org.junit.Test;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.web.Web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WildFlyWebTest {

    @Test
    public void testCanFindWebComponent() {
        WunderBoss.registerComponentProvider(new WildflyWebProvider(null));
        Web web = WunderBoss.findOrCreateComponent(Web.class);
        assertNotNull(web);
        assertTrue(web instanceof WildFlyWeb);
    }
}
