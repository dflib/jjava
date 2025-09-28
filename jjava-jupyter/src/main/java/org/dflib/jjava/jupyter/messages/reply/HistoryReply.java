package org.dflib.jjava.jupyter.messages.reply;

import org.dflib.jjava.jupyter.kernel.history.HistoryEntry;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.HistoryRequest;

import java.util.List;

public class HistoryReply implements ContentType<HistoryReply>, ReplyType<HistoryRequest> {
    public static final MessageType<HistoryReply> MESSAGE_TYPE = MessageType.HISTORY_REPLY;
    public static final MessageType<HistoryRequest> REQUEST_MESSAGE_TYPE = MessageType.HISTORY_REQUEST;

    @Override
    public MessageType<HistoryReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<HistoryRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    protected final List<HistoryEntry> history;

    public HistoryReply(List<HistoryEntry> history) {
        this.history = history;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }
}
