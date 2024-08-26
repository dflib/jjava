package org.dflib.jjava.jupyter.channels;

import java.io.ByteArrayOutputStream;
import java.util.function.BiConsumer;

public class JupyterOutputStream extends ByteArrayOutputStream {
    private static final int INITIAL_BUFFER_CAP = 1024;

    private ShellReplyEnvironment env;
    private final BiConsumer<ShellReplyEnvironment, String> write;

    public JupyterOutputStream(ShellReplyEnvironment env, BiConsumer<ShellReplyEnvironment, String> write) {
        super(INITIAL_BUFFER_CAP);
        this.env = env;
        this.write = write;
    }

    public JupyterOutputStream(BiConsumer<ShellReplyEnvironment, String> write) {
        this(null, write);
    }

    public void setEnv(ShellReplyEnvironment env) {
        this.env = env;
    }

    public void retractEnv(ShellReplyEnvironment env) {
        if (this.env == env)
            this.env = null;
    }

    public boolean isAttached() {
        return this.env != null;
    }

    @Override
    public void flush() {
        if (this.env != null) {
            String contents = new String(super.buf, 0, super.count, JupyterSocket.UTF_8);
            if (!contents.isEmpty())
                this.write.accept(this.env, contents);
        }

        super.reset();
    }
}
