package org.projectodd.wunderboss.ruby;

import org.jboss.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RubyLocator {

    public static List<File> locateLibs(String jrubyHome) {
        List<File> files = new ArrayList<>();
        if (jrubyHome != null) {
            File libDir = new File(jrubyHome, "lib");
            if (!libDir.exists()) {
                throw new RuntimeException("JRuby Home of " + jrubyHome + " does not appear to be a valid JRuby install");
            }

            for (File each : libDir.listFiles()) {
                if (each.getName().endsWith(".jar")) {
                    files.add(each);
                }
            }
        }

        return files;
    }

    private static final Logger log = Logger.getLogger(RubyLocator.class);
}
