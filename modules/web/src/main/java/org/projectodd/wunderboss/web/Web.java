/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

import javax.servlet.Servlet;
import java.util.Map;
import java.util.Set;

public interface Web<S> extends Component {
    class CreateOption extends Option {
        public static final CreateOption HOST       = opt("host", "localhost", CreateOption.class);
        public static final CreateOption PORT       = opt("port", 8080, CreateOption.class);
        public static final CreateOption AUTO_START = opt("auto_start", true, CreateOption.class);
    }

    class RegisterOption extends Option {
        public static final RegisterOption PATH       = opt("path", "/", RegisterOption.class);
        public static final RegisterOption STATIC_DIR = opt("static_dir", RegisterOption.class);
        public static final RegisterOption VHOSTS     = opt("vhosts", RegisterOption.class);
    }

    /**
     * Registers a handler.
     * @param handler
     * @param opts
     * @return true if this replaced an existing handler at the same context.
     */
    boolean registerHandler(S handler, Map<RegisterOption, Object> opts);

    /**
     * Registers a servlet.
     * @param servlet
     * @param opts
     * @return true if this replaced an existing servlet at the same context.
     */
    boolean registerServlet(Servlet servlet, Map<RegisterOption, Object> opts);

    /**
     * Unregisters a handler or servlet at the given context path,
     * possibly on a virtual host.
     * @param context
     * @return true if there was actually something to unregister.
     */
    boolean unregister(Map<RegisterOption, Object> opts);

    /**
     * @return an unmodifiable Set of contexts
     */
    Set<String> registeredContexts();

}
