package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.messages.MessageContext;
import org.dflib.jjava.jupyter.messages.publish.PublishStream;

public class ShellReplyEnvironment extends DefaultReplyEnvironment {
    private final StdinChannel stdin;

    private boolean requestShutdown = false;

    protected ShellReplyEnvironment(ShellChannel shell, StdinChannel stdin, JupyterSocket iopub, MessageContext context) {
        super(shell, iopub, context);
        this.stdin = stdin;
    }

    @Override
    public ShellReplyEnvironment defer() {
        super.defer();
        return this;
    }

    public void markForShutdown() {
        this.requestShutdown = true;
    }

    public boolean isMarkedForShutdown() {
        return this.requestShutdown;
    }

    public void writeToStdOut(String msg) {
        publish(new PublishStream(PublishStream.StreamType.OUT, msg));
    }

    public void writeToStdErr(String msg) {
        publish(new PublishStream(PublishStream.StreamType.ERR, msg));
    }

    public String readFromStdIn(String prompt, boolean isPassword) {
        return this.stdin.getInput(super.getContext(), prompt, isPassword);
    }

    public String readFromStdIn(String prompt) {
        return this.readFromStdIn(prompt, false);
    }

    public String readFromStdIn() {
        return this.readFromStdIn("", false);
    }
}
