package org.projectodd.wunderboss.messaging;

import java.util.Map;

public interface ReplyableMessage<T> extends Message<T> {

    Response reply(String content, String contentType,
                   Map<Connection.SendOption, Object> options) throws Exception;

    Response reply(byte[] content, String contentType,
                   Map<Connection.SendOption, Object> options) throws Exception;
}
