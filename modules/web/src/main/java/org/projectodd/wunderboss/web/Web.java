package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.Component;

import javax.servlet.Servlet;
import java.util.Map;

public interface Web<T, S> extends Component<T> {
    enum ComponentOption {
        HOST("host"),
        PORT("port");

        ComponentOption(String value) {
            this.value = value;
        }

        public String value;
    }

    enum RegisterOption {
        CONTEXT_PATH("context_path"),
        DESTROY("destroy"),
        INIT("init"),
        STATIC_DIR("static_dir");

        RegisterOption(String value) {
            this.value = value;
        }

        public String value;
    }



    Web registerHandler(S handler, Map<RegisterOption, Object> opts);

    Web registerServlet(Servlet servlet, Map<RegisterOption, Object> opts);

    Web unregister(String context);

}
