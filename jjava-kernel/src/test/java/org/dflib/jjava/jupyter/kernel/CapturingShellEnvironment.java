package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.channels.ShellReplyEnvironment;
import org.dflib.jjava.jupyter.messages.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test helper: records every message a kernel publishes. Lives in the kernel package so it can reach the protected
 * {@code setEnv} of {@link JupyterIO}.
 */
public class CapturingShellEnvironment extends ShellReplyEnvironment {

    public final List<Message<?>> published = Collections.synchronizedList(new ArrayList<>());

    private CapturingShellEnvironment() {
        super(null, null, null, null);
    }

    public static CapturingShellEnvironment attach(JupyterIO io) {
        CapturingShellEnvironment env = new CapturingShellEnvironment();
        io.setEnv(env);
        return env;
    }

    @Override
    public void publish(Message<?> message) {
        published.add(message);
    }
}
