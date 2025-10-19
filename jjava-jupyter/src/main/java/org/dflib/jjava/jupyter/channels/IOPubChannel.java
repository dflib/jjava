package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

public class IOPubChannel extends JupyterSocket {
    public IOPubChannel(ZMQ.Context context, HMACGenerator hmacGenerator) {
        super(context, SocketType.PUB, hmacGenerator, LoggerFactory.getLogger("IOPubChannel"));
    }

    @Override
    public void bind(KernelConnectionProperties connProps) {
        String addr = formatAddress(connProps.getTransport(), connProps.getIp(), connProps.getIopubPort());

        logger.debug("Binding iopub to {}.", addr);
        super.bind(addr);
    }
}
