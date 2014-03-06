package org.projectodd.wunderboss.ruby;

import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RubyLanguageTest {
    String testApp = "src/test/resources/apps/basic";
    Language ruby;

    @Before
    public void setUp() throws Exception {
        WunderBoss.putOption("root", new File(testApp).getAbsolutePath());
        ruby = WunderBoss.findLanguage("ruby");
    }

    @Test
    public void testCanFindLanguage() {
        assertNotNull(ruby);
    }


    @Test
    public void testCanEvalCode() {
        assertEquals("it works!", ruby.eval("require 'ham';Ham.new.biscuit").toString());
    }
}
