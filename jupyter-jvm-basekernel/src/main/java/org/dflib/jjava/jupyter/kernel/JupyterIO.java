package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.channels.JupyterInputStream;
import org.dflib.jjava.jupyter.channels.JupyterOutputStream;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.channels.ShellReplyEnvironment;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class JupyterIO {
    private final JupyterOutputStream jupyterOut;
    private final JupyterOutputStream jupyterErr;
    private final JupyterInputStream jupyterIn;

    public final DisplayStream display;

    public final PrintStream out;
    public final PrintStream err;
    public final InputStream in;

    public JupyterIO(Charset encoding) {
        this.jupyterOut = new JupyterOutputStream(ShellReplyEnvironment::writeToStdOut);
        this.jupyterErr = new JupyterOutputStream(ShellReplyEnvironment::writeToStdErr);
        this.jupyterIn = new JupyterInputStream(encoding);

        this.display = new DisplayStream();

        try {
            this.out = new PrintStream(this.jupyterOut, true, encoding.name());
            this.err = new PrintStream(this.jupyterErr, true, encoding.name());
            this.in = this.jupyterIn;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Couldn't lookup the charset by name even though it is already a charset...", e);
        }
    }

    public JupyterIO() {
        this(JupyterSocket.UTF_8);
    }

    public boolean isAttached() {
        return this.jupyterOut.isAttached()
                && this.jupyterErr.isAttached()
                && this.jupyterIn.isAttached()
                && this.display.isAttached();
    }

    protected void setEnv(ShellReplyEnvironment env) {
        this.jupyterOut.setEnv(env);
        this.jupyterErr.setEnv(env);
        this.jupyterIn.setEnv(env);
        this.display.setEnv(env);
    }

    protected void retractEnv(ShellReplyEnvironment env) {
        this.jupyterOut.retractEnv(env);
        this.jupyterErr.retractEnv(env);
        this.jupyterIn.retractEnv(env);
        this.display.retractEnv(env);
    }

    protected void setJupyterInEnabled(boolean enabled) {
        this.jupyterIn.setEnabled(enabled);
    }
}
