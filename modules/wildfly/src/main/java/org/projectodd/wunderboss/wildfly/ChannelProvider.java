package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;

public class ChannelProvider implements ComponentProvider<ChannelWrapper> {

    @Override
    public ChannelWrapper create(String name, Options options) {
        return new ChannelWrapper(name);
    }
}
