package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IOPubChannel extends JupyterSocket {
    public IOPubChannel(ZMQ.Context context, HMACGenerator hmacGenerator) {
        super(context, SocketType.PUB, hmacGenerator, Logger.getLogger("IOPubChannel"));
    }

    @Override
    public void bind(KernelConnectionProperties connProps) {
        String addr = formatAddress(connProps.getTransport(), connProps.getIp(), connProps.getIopubPort());

        logger.log(Level.INFO, String.format("Binding iopub to %s.", addr));
        super.bind(addr);
    }
}
