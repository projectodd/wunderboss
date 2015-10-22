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

package org.projectodd.wunderboss.clojure;

import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.Utils;
import org.projectodd.wunderboss.WunderBoss;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        WunderBoss.updateClassPath(Utils.classpathStringToURLS(cp));

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
