package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.Component;
import java.util.Map;

public interface WebComponent<T, S> extends Component<T> {

    void registerHandler(String context, S handler, Map<String, Object> opts);

    void registerServlet(String context, Class servletClass, Map<String, Object> opts);

    boolean unregister(String context);

}
