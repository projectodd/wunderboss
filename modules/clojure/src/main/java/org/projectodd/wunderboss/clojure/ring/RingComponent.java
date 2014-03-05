package org.projectodd.wunderboss.clojure.ring;

import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentInstance;
import org.projectodd.wunderboss.LoaderWrapper;
import org.projectodd.wunderboss.Options;

public class RingComponent extends Component{
    @Override
    public String[] getLanguageDependencies() {
        return new String[]{"clojure"};
    }

    @Override
    public String[] getComponentDependencies() {
        return new String[]{"web"};
    }

    @Override
    public void boot() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void configure(Options options) {
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context", "/");
        String handler = options.getString("ring-handler");
        Runnable init  = (Runnable) options.get("init");

        //TODO: make an InvalidOptionException?
        if (handler == null) {
            throw new RuntimeException("'ring-handler' option missing for ring component");
        }

        try {
            RingHandler ringHandler = new RingHandler((LoaderWrapper)application.runtime(), handler);

            Options webOptions = new Options();
            webOptions.put("context", context);
            webOptions.put("http_handler", ringHandler);
            //webOptions.put("static_dir", staticDirectory);
            ComponentInstance web = application.start("web", webOptions);
            if (init != null) { init.run(); } // before or after?
            Options instanceOptions = new Options();
            instanceOptions.put("web", web);
            instanceOptions.put("destroy", options.get("destroy"));
            return new ComponentInstance(this, instanceOptions);
        } catch (Exception e) {
            // TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance web = (ComponentInstance) instance.getOptions().get("web");
        web.stop();
        Runnable destroy = (Runnable) instance.getOptions().get("destroy");
        if (destroy != null) { destroy.run(); }
    }

}
