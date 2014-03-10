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
        for(String each : cp.trim().split(":")) {
            files.add(new File(each));
        }

        return files;
    }
}
