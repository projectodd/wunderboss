package org.projectodd.wunderboss.web.async;

import java.io.IOException;

public interface Channel {
    void notifyOpen(Object context);

    boolean isOpen();

    boolean send(Object message) throws Exception;

    boolean send(Object message, boolean shouldClose) throws Exception;
    
    void close() throws IOException;

    interface OnOpen {
        void handle(Channel channel, Object context);
    }

    interface OnClose {
        void handle(Channel channel, int code, String reason);
    }
}
