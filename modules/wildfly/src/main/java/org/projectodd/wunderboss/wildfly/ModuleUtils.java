package org.projectodd.wunderboss.wildfly;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

public class ModuleUtils {

    public static void addToModuleClasspath(Module module, List<File> urls) throws IOException, ModuleLoadException {
        List<ResourceLoaderSpec> loaderSpecs = new ArrayList<>();
        for (File each : urls) {
            String name = each.getName();
            ResourceLoader loader = name.endsWith(".jar") ?
                    ResourceLoaders.createJarResourceLoader(name, new JarFile(each)) :
                    ResourceLoaders.createFileResourceLoader(name, each);
            loaderSpecs.add(ResourceLoaderSpec.createResourceLoaderSpec(loader));
        }

        for (ResourceLoader each : getExistingResourceLoaders(module)) {
            loaderSpecs.add(ResourceLoaderSpec.createResourceLoaderSpec(each));
        }

        ModuleLoader moduleLoader = module.getModuleLoader();

        try {
            lookupMethod(ModuleLoader.class, "setAndRefreshResourceLoaders", Module.class, Collection.class)
                    .invoke(moduleLoader, module, loaderSpecs);
            lookupMethod(ModuleLoader.class, "refreshResourceLoaders", Module.class)
                    .invoke(moduleLoader, module);
            lookupMethod(ModuleLoader.class, "relink", Module.class)
                    .invoke(moduleLoader, module);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static ResourceLoader[] getExistingResourceLoaders(Module module) {
        ResourceLoader[] loaders;
        try {
            loaders = (ResourceLoader[])lookupMethod(ModuleClassLoader.class, "getResourceLoaders")
                    .invoke(module.getClassLoader());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return loaders;
    }

    protected static Method lookupMethod(Class clazz, String methodName, Class... argClasses) {
        Method method;
        try {
        method = clazz.getDeclaredMethod(methodName, argClasses);
        method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }

        return method;
    }

}

