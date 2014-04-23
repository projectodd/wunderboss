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

package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

import java.util.Map;

public interface Messaging<T, E, C> extends Component<T> {

    class CreateOption extends Option {
        public static final CreateOption HOST = opt("host", CreateOption.class);
        public static final CreateOption PORT = opt("port", CreateOption.class);

        /**
         * Specifies if xa is on by default. Defaults to false.
         */
        public static final CreateOption XA = opt("xa", false, CreateOption.class);
    }

    class CreateEndpointOption extends Option {
        public static final CreateEndpointOption BROADCAST = opt("broadcast", false, CreateEndpointOption.class);
        public static final CreateEndpointOption DURABLE   = opt("durable", true, CreateEndpointOption.class);
        public static final CreateEndpointOption SELECTOR  = opt("selector", CreateEndpointOption.class);
    }

    Endpoint<E> findOrCreateEndpoint(String name,
                                     Map<CreateEndpointOption, Object> options) throws Exception;

    class CreateConnectionOption extends Option {
        /**
         * If true, and xa connection is returned. Defaults to whatever was specified for
         * CreateOption.XA.
         */
        public static final CreateConnectionOption XA = opt("xa", CreateConnectionOption.class);
    }

    //TODO: remote connections?
    Connection<C> createConnection(Map<CreateConnectionOption, Object> options) throws Exception;

    boolean isXaDefault();
}
