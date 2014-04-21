package org.projectodd.wunderboss.messaging;

public interface MessageHandler {
    void onMessage(Message msg) throws Exception;
}
