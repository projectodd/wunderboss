package org.projectodd.wunderboss.wildfly;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ruby.RubyLanguage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

public class WildFlyRubyLanguage extends RubyLanguage {

    @Override
    public void initialize(WunderBoss container) {
        super.initialize(container);

        jrubyHome = "/home/bbrowning/.rbenv/versions/jruby-1.7.10";
        File libDir = new File(jrubyHome, "lib");
        List<ResourceLoaderSpec> loaderSpecs = new ArrayList<ResourceLoaderSpec>();
        for (File child : libDir.listFiles()) {
            if (child.getName().endsWith(".jar")) {
                try {
                    ResourceLoader loader = ResourceLoaders.createJarResourceLoader(child.getName(), new JarFile(child));
                    loaderSpecs.add(ResourceLoaderSpec.createResourceLoaderSpec(loader));
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        swizzleResourceLoaders(loaderSpecs);

        // eval("require 'bundler/setup'");
    }

    @Override
    protected Ruby createRuntime(String root) {
        RubyInstanceConfig rubyConfig = new RubyInstanceConfig();
        rubyConfig.setLoadPaths(Arrays.asList(root));
        rubyConfig.setJRubyHome(jrubyHome);
        return Ruby.newInstance(rubyConfig);
    }

    private static void swizzleResourceLoaders(List<ResourceLoaderSpec> loaderSpecs) {
        try {
            Module module = Module.forClass(WildFlyRubyLanguage.class);

            List<ResourceLoaderSpec> specs = new ArrayList<ResourceLoaderSpec>();
            specs.addAll(loaderSpecs);

            ModuleLoader moduleLoader = module.getModuleLoader();

            for (ResourceLoader each : getExistingResourceLoaders(module)) {
                specs.add(ResourceLoaderSpec.createResourceLoaderSpec(each));
            }

            Method method = ModuleLoader.class.getDeclaredMethod("setAndRefreshResourceLoaders", Module.class, Collection.class);
            method.setAccessible(true);
            method.invoke(moduleLoader, module, specs);

            Method refreshMethod = ModuleLoader.class.getDeclaredMethod("refreshResourceLoaders", Module.class);
            refreshMethod.setAccessible(true);
            refreshMethod.invoke(moduleLoader, module);

            Method relinkMethod = ModuleLoader.class.getDeclaredMethod("relink", Module.class);
            relinkMethod.setAccessible(true);
            relinkMethod.invoke(moduleLoader, module);
        } catch (Exception e) {
            log.fatal(e.getMessage(), e);
        }

    }

    private static ResourceLoader[] getExistingResourceLoaders(Module module) throws Exception {
        ModuleClassLoader cl = module.getClassLoader();

        Method method = ModuleClassLoader.class.getDeclaredMethod("getResourceLoaders");
        method.setAccessible(true);
        Object result = method.invoke(cl);

        return (ResourceLoader[]) result;

    }

    private String jrubyHome;

    private static final Logger log = Logger.getLogger(WildFlyRubyLanguage.class);
}
