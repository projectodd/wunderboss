/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.web.async;

import org.projectodd.wunderboss.web.Attachments;

import java.io.IOException;

public interface Channel extends Attachments {
    void notifyOpen(Object context);

    void notifyError(Throwable error);

    boolean isOpen();

    boolean send(Object message, boolean shouldClose, OnComplete callback) throws Exception;
    
    void close() throws IOException;

    interface OnOpen {
        void handle(Channel channel, Object context);
    }

    interface OnClose {
        void handle(Channel channel, Object code, String reason);
    }

    interface OnError {
        void handle(Channel channel, Throwable error);
    }

    interface OnComplete {
        void handle(Throwable error);
    }
}
