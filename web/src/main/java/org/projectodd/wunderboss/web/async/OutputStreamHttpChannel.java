/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class OutputStreamHttpChannel implements HttpChannel {

    public OutputStreamHttpChannel(final OnOpen onOpen, final OnError onError,
                                   final OnClose onClose) {
        this.onOpen = onOpen;
        this.onError = onError;
        this.onClose = onClose;
    }

    protected abstract String getResponseCharset();

    protected abstract void setContentLength(int length);

    protected abstract OutputStream getOutputStream() throws IOException;

    protected abstract void execute(Runnable runnable);

    @Override
    public Object get(Object key) {
        return this.attachments.get(key);
    }

    @Override
    public void attach(Object key, Object value) {
        this.attachments.put(key, value);
    }

    @Override
    public void notifyOpen(final Object context) {
        if (!isOpen()) {
            this.open = true;
            if (this.onOpen != null) {
                onOpen.handle(this, context);
            }
        }
    }

    @Override
    public void notifyError(Throwable error) {
        if (this.onError != null) {
            this.onError.handle(this, error);
        }
    }

    protected void notifyClose() {
        if (!this.closeNotified &&
                this.onClose != null) {
            this.closeNotified = true;
            this.onClose.handle(this, null, null);
        }
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean sendStarted() {
        return this.sendQueued;
    }

    // message must be String or byte[]. Allowing Object makes life easier from clojure
    @Override
    public synchronized boolean send(final Object message,
                                     final boolean shouldClose,
                                     final OnComplete onComplete) throws IOException {
        if (!isOpen()) {
            return false;
        }

        this.sendQueued = true;

        byte[] data;
        if (message == null) {
            data = null;
        } else if (message instanceof String) {
            data = ((String)message).getBytes(getResponseCharset());
        } else if (message instanceof byte[]) {
            data = (byte[])message;
        } else {
            throw WebsocketUtil.wrongMessageType(message.getClass());
        }

        enqueue(new PendingSend(data, shouldClose, onComplete));

        return true;
    }

    protected void send(PendingSend pending) {
        doSend(pending.message, pending.shouldClose, pending.onComplete);
    }

    //TODO: convert to do/while?
    private final Runnable pumpWorker = new Runnable() {
        @Override
        public void run() {
            PendingSend pending;
            synchronized (workerRunning) {
                pending = queue.poll();
                if (pending == null) {
                    workerRunning.set(false);
                }
            }
            while (pending != null) {
                try {
                    send(pending);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (workerRunning) {
                    pending = queue.poll();
                    if (pending == null) {
                        workerRunning.set(false);
                    }
                }
            }

        }
    };

    void enqueue(PendingSend data) {
        synchronized (workerRunning) {
            queue.add(data);
            if (workerRunning.compareAndSet(false, true)) {
                execute(pumpWorker);
            }
        }
    }

    protected void doSend(final byte[] data,
                          final boolean shouldClose,
                          final OnComplete onComplete) {
        Throwable ex = null;
        try {
            if (!headersSent) {
                if (shouldClose) {
                    if (data == null) {
                        setContentLength(0);
                    } else {
                        setContentLength(data.length);
                    }
                }
                this.stream = getOutputStream();
                headersSent = true;
            }

            if (data != null) {
                this.stream.write(data);
            }

            if (shouldClose) {
                this.closer.run();
            } else {
                this.stream.flush();
            }
        } catch (Throwable e) {
            this.closer.run();
            ex = e;
        }

        WebsocketUtil.notifyComplete(this, onComplete, ex);
    }

    @Override
    public void close() throws IOException {
        if (!this.open) {
            return;
        }

        if (this.stream == null) {
            this.stream = getOutputStream();
        }

        this.stream.close();
        this.open = false;

        notifyClose();
    }

    protected Runnable closer = new Runnable() {
        @Override
        public void run() {
            try {
                close();
            } catch (IOException ignored) {
                // undertow throws when you close with unwritten data,
                // but the data can never be written - see UNDERTOW-368
            }
        }
    };

    private boolean open = false;
    private boolean sendQueued = false;
    private boolean headersSent = false;
    private boolean closeNotified = false;
    private OutputStream stream;
    private final OnOpen onOpen;
    private final OnError onError;
    private final OnClose onClose;
    private final Queue<PendingSend> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);
    private final Map<Object, Object> attachments = new ConcurrentHashMap<>();

    class PendingSend {
        PendingSend(final byte[] message,
                    final boolean shouldClose,
                    final OnComplete onComplete) {
            this.message = message;
            this.shouldClose = shouldClose;
            this.onComplete = onComplete;
        }

        public final byte[] message;
        public final boolean shouldClose;
        public final OnComplete onComplete;
    }
}
