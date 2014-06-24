package org.projectodd.wunderboss.messaging;

import java.util.Map;

public class ConcreteReply implements Reply {
    public ConcreteReply(Object content, Map<String, Object> properties) {
        this.content = content;
        this.properties = properties;
    }

    @Override
    public Object content() {
        return this.content;
    }

    @Override
    public Map<String, Object> properties() {
        return this.properties;
    }

    private final Object content;
    private final Map<String, Object> properties;
}
