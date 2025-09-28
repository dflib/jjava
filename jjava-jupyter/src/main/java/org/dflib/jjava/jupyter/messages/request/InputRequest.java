package org.dflib.jjava.jupyter.messages.request;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.RequestType;
import org.dflib.jjava.jupyter.messages.reply.InputReply;

public class InputRequest implements ContentType<InputRequest>, RequestType<InputReply> {
    public static final MessageType<InputRequest> MESSAGE_TYPE = MessageType.INPUT_REQUEST;
    public static final MessageType<InputReply> REPLY_MESSAGE_TYPE = MessageType.INPUT_REPLY;

    @Override
    public MessageType<InputRequest> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<InputReply> getReplyType() {
        return REPLY_MESSAGE_TYPE;
    }

    protected String prompt;
    protected boolean password;

    public InputRequest(String prompt, boolean password) {
        this.prompt = prompt;
        this.password = password;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isPassword() {
        return password;
    }
}
