package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageContext;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.publish.PublishStatus;
import org.zeromq.ZMQ;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JupyterConnection {
    private final KernelConnectionProperties connProps;

    private boolean isConnected = false;
    private final ZMQ.Context ctx;

    protected final HeartbeatChannel heartbeat;
    protected final ShellChannel shell;
    protected final ShellChannel control;
    protected final StdinChannel stdin;
    protected final IOPubChannel iopub;

    private final Map<MessageType, ShellHandler> handlers;

    public JupyterConnection(KernelConnectionProperties connProps) throws NoSuchAlgorithmException, InvalidKeyException {
        this.connProps = connProps;
        this.ctx = ZMQ.context(1);

        HMACGenerator hmacGenerator = connProps.createHMACGenerator();

        this.heartbeat = new HeartbeatChannel(this.ctx, hmacGenerator);
        this.shell = new ShellChannel(this.ctx, hmacGenerator, false, this);
        this.control = new ShellChannel(this.ctx, hmacGenerator, true, this);
        this.stdin = new StdinChannel(this.ctx, hmacGenerator);
        this.iopub = new IOPubChannel(this.ctx, hmacGenerator);

        this.handlers = new HashMap<>();
    }

    public void connect() {
        if (!isConnected) {
            forEachSocket(s -> s.bind(this.connProps));
            PublishStatus publishStatus = PublishStatus.STARTING;
            this.getIOPub().sendMessage(new Message<>(null, PublishStatus.MESSAGE_TYPE, publishStatus));
            this.isConnected = true;
        }
    }

    public IOPubChannel getIOPub() {
        return this.iopub;
    }

    public <T> void setHandler(MessageType<T> type, ShellHandler<T> handler) {
        this.handlers.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public <T> ShellHandler<T> getHandler(MessageType<T> type) {
        return this.handlers.get(type);
    }

    public ShellReplyEnvironment prepareReplyEnv(ShellChannel shell, MessageContext context) {
        return new ShellReplyEnvironment(shell, this.stdin, this.iopub, context);
    }

    private void forEachSocket(Consumer<JupyterSocket> consumer) {
        consumer.accept(this.heartbeat);
        consumer.accept(this.shell);
        consumer.accept(this.control);
        consumer.accept(this.stdin);
        consumer.accept(this.iopub);
    }

    public void close() {
        forEachSocket(JupyterSocket::close);
        this.ctx.close();
    }

    public void waitUntilClose() {
        forEachSocket(JupyterSocket::waitUntilClose);
    }
}
