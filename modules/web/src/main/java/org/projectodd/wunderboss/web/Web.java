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

import javax.servlet.Servlet;
import java.util.Map;

public interface Web<T, S> extends Component<T> {
    enum CreateOption {
        HOST("host"),
        PORT("port");

        CreateOption(String value) {
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
