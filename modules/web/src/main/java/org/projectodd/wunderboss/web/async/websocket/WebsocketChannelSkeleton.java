package org.projectodd.wunderboss.web.async.websocket;

public abstract class WebsocketChannelSkeleton implements WebsocketChannel {


    public WebsocketChannelSkeleton(final OnOpen onOpen, final OnClose onClose, final OnMessage onMessage, final OnError onError) {
        this.onOpen = onOpen;
        this.onClose = onClose;
        this.onMessage = onMessage;
        this.onError = onError;
    }

    @Override
    public void notifyOpen(final Object context) {
        if (!this.openNotified &&
                this.onOpen != null) {
            this.onOpen.handle(this, context);
        }
        this.openNotified = true;
    }

    @Override
    public boolean send(Object message) throws Exception {
        return send(message, false);
    }

    protected void notifyClose(int code, String reason) {
        if (!closeNotified &&
                this.onClose != null) {
            this.onClose.handle(this, code, reason);
        }
        closeNotified = true;
    }

    protected void notifyMessage(Object message) {
        if (this.onMessage != null) {
            this.onMessage.handle(this, message);
        }
    }

    protected void notifyError(Throwable error) {
        if (this.onError != null) {
            this.onError.handle(this, error);
        }
    }

    private final OnOpen onOpen;
    private final OnClose onClose;
    private final OnMessage onMessage;
    private final OnError onError;
    private boolean closeNotified = false;
    private boolean openNotified = false;
}
