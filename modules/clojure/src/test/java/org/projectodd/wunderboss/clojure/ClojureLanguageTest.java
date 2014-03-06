package org.projectodd.wunderboss.clojure;

import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClojureLanguageTest {
    String testApp = "src/test/resources/apps/basic";
    String classpathFile = "target/apps/basic/lein-classpath";
    Language clojure;

    @Before
    public void setUp() throws Exception {
        WunderBoss.putOption("root", testApp);
        byte[] data = Files.readAllBytes(Paths.get(classpathFile));
        String cp = Charset.defaultCharset().decode(ByteBuffer.wrap(data)).toString();
        List<URL> urls = new ArrayList<>();

        for(String each : cp.split(":")) {
            urls.add(new File(each).toURI().toURL());
        }

        WunderBoss.updateClassPath(urls);

        clojure = WunderBoss.findLanguage("clojure");
    }

    @Test
    public void testCanFindLanguage() {
        assertNotNull(clojure);
    }


    @Test
    public void testCanEvalCode() {
        assertEquals("it works!", clojure.eval("(require 'basic.core)(basic.core/test-fn)"));
    }
}
