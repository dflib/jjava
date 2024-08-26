package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageContext;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.publish.PublishStatus;
import org.dflib.jjava.jupyter.messages.reply.ErrorReply;

import java.util.Deque;
import java.util.LinkedList;

public class DefaultReplyEnvironment implements ReplyEnvironment {
    private final JupyterSocket shell;
    private final JupyterSocket iopub;

    private final MessageContext context;

    private Deque<Runnable> deferred = new LinkedList<>();
    private boolean defer = false;

    public DefaultReplyEnvironment(JupyterSocket shell, JupyterSocket iopub, MessageContext context) {
        this.shell = shell;
        this.iopub = iopub;
        this.context = context;
    }

    public JupyterSocket getShell() {
        return shell;
    }

    public JupyterSocket getIopub() {
        return iopub;
    }

    public MessageContext getContext() {
        return context;
    }

    @Override
    public void publish(Message<?> msg) {
        if (defer) {
            deferred.push(() -> iopub.sendMessage(msg));
            this.defer = false;
        } else {
            iopub.sendMessage(msg);
        }
    }

    @Override
    public void reply(Message<?> msg) {
        if (defer) {
            deferred.push(() -> shell.sendMessage(msg));
            this.defer = false;
        } else {
            shell.sendMessage(msg);
        }
    }

    @Override
    public ReplyEnvironment defer() {
        this.defer = true;
        return this;
    }

    @Override
    public void defer(Runnable action) {
        this.deferred.push(action);
    }

    @Override
    public void resolveDeferrals() {
        if (this.defer)
            throw new IllegalStateException("Reply environment is in defer mode but a resolution was request.");

        while (!deferred.isEmpty())
            deferred.pop().run();
    }

    @Override
    public <T extends ContentType<T>> void publish(T content) {
        publish(new Message<>(context, content.getType(), content));
    }

    @Override
    public <T extends ContentType<T>> void reply(T content) {
        reply(new Message<>(context, content.getType(), content));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void replyError(MessageType<?> type, ErrorReply error) {
        reply(new Message(context, type, error));
    }

    @Override
    public void setStatusBusy() {
        publish(PublishStatus.BUSY);
    }

    @Override
    public void setStatusIdle() {
        publish(PublishStatus.IDLE);
    }

    @Override
    public void setBusyDeferIdle() {
        setStatusBusy();
        defer().setStatusIdle();
    }
}
