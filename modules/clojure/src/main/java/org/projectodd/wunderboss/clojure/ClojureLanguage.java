package org.projectodd.wunderboss.clojure;

import org.projectodd.shimdandy.ClojureRuntimeShim;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClojureLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {
        this.container = container;
    }

    @Override
    public synchronized ClojureRuntimeShim runtime() {
        if (this.runtime == null) {
            List classpath = new ArrayList((List)this.container.options().get("classpath", Collections.EMPTY_LIST));
            classpath.add(implJarURL());
            URLClassLoader wrappingLoader = URLClassLoader.newInstance((URL[])classpath
                    .toArray(new URL[classpath.size()]), this.container.classLoader());

            this.runtime = ClojureRuntimeShim.newRuntime(wrappingLoader);
        }

        return this.runtime;
    }

    @Override
    public synchronized void shutdown() {
        if (this.runtime != null) {
            this.runtime.invoke("clojure.core/shutdown-agents");
        }
    }

    @Override
    public Object eval(String strToEval) {
        return runtime().invoke("clojure.core/eval", runtime().invoke("clojure.core/read-string", strToEval));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    // copy jar-within-jar to a tmp file so we can access it from a URLClassLoader. this is fucking crippled
    private synchronized URL implJarURL() {
        if (this.implJar == null) {

            InputStream in = null;
            OutputStream out = null;
            try {
                File tmpJar = File.createTempFile("shimdandy-impl", "jar");
                tmpJar.deleteOnExit();
                in = this.getClass().getClassLoader().getResourceAsStream("shimdandy-impl.jar");
                out = new FileOutputStream(tmpJar);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                this.implJar = tmpJar.toURI().toURL();
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (IOException ffs) {}
            }
        }
        return this.implJar;
    }

    private URL implJar = null;
    private WunderBoss container;
    private ClojureRuntimeShim runtime;
}


