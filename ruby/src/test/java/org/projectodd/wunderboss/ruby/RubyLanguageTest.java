/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.ruby;

import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RubyLanguageTest {
    String testApp = RubyLanguageTest.class.getClassLoader().getResource("apps/basic").getPath();
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
        assertEquals("it works!", ruby.eval("puts $:.inspect;require 'ham';Ham.new.biscuit").toString());
    }
}
