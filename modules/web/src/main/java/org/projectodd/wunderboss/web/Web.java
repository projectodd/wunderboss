package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.Component;
import java.util.Map;
import javax.servlet.Servlet;

public interface Web<T, S> extends Component<T> {

    void registerHandler(S handler, Map<String, Object> opts);

    void registerServlet(Servlet servlet, Map<String, Object> opts);

    boolean unregister(String context);

}
