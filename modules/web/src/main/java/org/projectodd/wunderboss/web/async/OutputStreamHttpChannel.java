package org.projectodd.wunderboss.web.async;

import org.projectodd.wunderboss.web.async.websocket.Util;

import java.io.IOException;
import java.io.OutputStream;

public abstract class OutputStreamHttpChannel implements HttpChannel {

    public OutputStreamHttpChannel(final OnOpen onOpen, final OnClose onClose) {
        this.onOpen = onOpen;
        this.onClose = onClose;
    }

    protected abstract String getResponseCharset();

    protected abstract void setContentLength(int length);

    protected abstract OutputStream getOutputStream() throws Exception;

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
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean send(final Object message) throws Exception {
        return send(message, true);
    }

    // message must be String or byte[]. Allowing Object makes life easier from clojure
    @Override
    public boolean send(final Object message, final boolean shouldClose) throws Exception {
        if (!isOpen()) {
            return false;
        }

        byte[] data;
        if (message instanceof String) {
            data = ((String)message).getBytes(getResponseCharset());
        } else if (message instanceof byte[]) {
            data = (byte[])message;
        } else {
            throw Util.wrongMessageType(message.getClass());
        }

        if (!sendStarted) {
            if (shouldClose) {
                setContentLength(data.length);
            }
            this.stream = getOutputStream();
            sendStarted = true;
        }

        try {
            this.stream.write(data);
            if (!shouldClose) {
                this.stream.flush();
            }
        } catch (IOException e) {
            // TODO: should we only deal with "Broken pipe" IOE's here? rethrow others?
            try {
                this.close();
            } catch (IOException ignored) {
                // undertow throws when you close with unwritten data,
                // but the data can never be written - see UNDERTOW-368
            }
        }

        if (shouldClose) {
            this.close();
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        if (this.stream != null) {
            this.stream.close();
        }
        this.open = false;

        if (this.onClose != null) {
            this.onClose.handle(this, NORMAL_CLOSURE, null);
        }
    }

    private boolean open = false;
    private boolean sendStarted = false;
    private OutputStream stream;
    private final OnOpen onOpen;
    private final OnClose onClose;
}
