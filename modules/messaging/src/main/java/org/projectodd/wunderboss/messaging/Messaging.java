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
