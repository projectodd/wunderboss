package org.projectodd.wunderboss.messaging;

import java.util.Map;

public interface Reply {

    Object content();

    Map<String, Object> properties();
}
