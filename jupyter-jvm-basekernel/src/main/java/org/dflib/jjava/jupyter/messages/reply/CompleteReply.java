package org.dflib.jjava.jupyter.messages.reply;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.CompleteRequest;

import java.util.List;
import java.util.Map;

public class CompleteReply implements ContentType<CompleteReply>, ReplyType<CompleteRequest> {
    public static final MessageType<CompleteReply> MESSAGE_TYPE = MessageType.COMPLETE_REPLY;
    public static final MessageType<CompleteRequest> REQUEST_MESSAGE_TYPE = MessageType.COMPLETE_REQUEST;

    @Override
    public MessageType<CompleteReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<CompleteRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    protected final String status = "ok";

    protected final List<String> matches;

    /**
     * The starting position in the request's code to replace with a match
     */
    @SerializedName("cursor_start")
    protected final int cursorStart;

    /**
     * The end position in the request's code to replace with a match
     */
    @SerializedName("cursor_end")
    protected final int cursorEnd;

    protected final Map<String, Object> metadata;

    public CompleteReply(List<String> matches, int cursorStart, int cursorEnd, Map<String, Object> metadata) {
        this.matches = matches;
        this.cursorStart = cursorStart;
        this.cursorEnd = cursorEnd;
        this.metadata = metadata;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getMatches() {
        return matches;
    }

    public int getCursorStart() {
        return cursorStart;
    }

    public int getCursorEnd() {
        return cursorEnd;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
