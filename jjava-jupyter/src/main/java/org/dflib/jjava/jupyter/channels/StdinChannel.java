package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageContext;
import org.dflib.jjava.jupyter.messages.reply.InputReply;
import org.dflib.jjava.jupyter.messages.request.InputRequest;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StdinChannel extends JupyterSocket {
    public StdinChannel(ZMQ.Context context, HMACGenerator hmacGenerator) {
        super(context, SocketType.ROUTER, hmacGenerator, Logger.getLogger("StdinChannel"));
    }

    @Override
    public void bind(KernelConnectionProperties connProps) {
        String addr = formatAddress(connProps.getTransport(), connProps.getIp(), connProps.getStdinPort());

        logger.log(Level.INFO, String.format("Binding stdin to %s.", addr));
        super.bind(addr);
    }

    /**
     * Ask the frontend for input.
     * <p>
     * <strong>Do not ask for input if an execute request has `allow_stdin=False`</strong>
     *
     * @param context           a message that the request with input was invoked by such as an execute request
     * @param prompt            a prompt string for the front end to include with the input request
     * @param isPasswordRequest a flag specifying if the input request is for a password, if so
     *                          the frontend should obscure the user input (for example with password
     *                          dots or not echoing the input)
     *
     * @return the input string from the frontend.
     */
    public synchronized String getInput(MessageContext context, String prompt, boolean isPasswordRequest) {
        InputRequest content = new InputRequest(prompt, isPasswordRequest);
        Message<InputRequest> request = new Message<>(context, InputRequest.MESSAGE_TYPE, content);

        super.sendMessage(request);

        Message<InputReply> reply = super.readMessage(InputReply.MESSAGE_TYPE);

        return reply.getContent().getValue() + System.lineSeparator();
    }
}
