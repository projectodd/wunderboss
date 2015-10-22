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

package org.projectodd.wunderboss;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<URL> classpathStringToURLS(String cp) {
        List<URL> urls = new ArrayList<>();
        try {
            for (File each : classpathStringToFiles(cp)) {
                urls.add(each.toURI().toURL());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return urls;
    }

    public static List<File> classpathStringToFiles(String cp) {
        List<File> files = new ArrayList<>();
        for(String each : classpathStringToArray(cp)) {
            files.add(new File(each));
        }

        return files;
    }
    
    public static String[] classpathStringToArray(String cp) {
        cp = cp.trim();
        String[] entries = cp.split(":");
        // Basic splitting on ":" may break some entries under Windows of the
        // form C:\foo\bar.jar
        List<String> results = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if (i + 1 < entries.length) {
                String nextEntry = entries[i + 1];
                if (entry.matches("[A-Za-z]") && nextEntry.charAt(0) == '\\') {
                    // We split up a path containing a Windows drive letter so
                    // reassemble it
                    results.add(entry + ":" + nextEntry);
                    i++;
                    continue;
                }
            }
            results.add(entry);
        }
        return results.toArray(new String[results.size()]);
    }

    public static void deleteRecursively(File directory) {
        if (directory.isDirectory()) {
            File[] children = directory.listFiles();
            if (children != null) {
                for (File file : children) {
                    deleteRecursively(file);
                }
            }
        }
        directory.delete();
    }
}
