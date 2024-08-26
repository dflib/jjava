package org.dflib.jjava.jupyter.messages.reply;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.request.IsCompleteRequest;

public class IsCompleteReply implements ContentType<IsCompleteReply>, ReplyType<IsCompleteRequest> {
    public static final MessageType<IsCompleteReply> MESSAGE_TYPE = MessageType.IS_COMPLETE_REPLY;
    public static final MessageType<IsCompleteRequest> REQUEST_MESSAGE_TYPE = MessageType.IS_COMPLETE_REQUEST;

    @Override
    public MessageType<IsCompleteReply> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<IsCompleteRequest> getRequestType() {
        return REQUEST_MESSAGE_TYPE;
    }

    public static final IsCompleteReply VALID_CODE = new IsCompleteReply(Status.VALID_CODE);
    public static final IsCompleteReply INVALID_CODE = new IsCompleteReply(Status.INVALID_CODE);
    public static final IsCompleteReply UNKNOWN = new IsCompleteReply(Status.UNKNOWN);

    private static final IsCompleteReply[] COMMON_INDENTS = {
            new IsCompleteReply(Status.NOT_FINISHED, ""),
            new IsCompleteReply(Status.NOT_FINISHED, " "),
            new IsCompleteReply(Status.NOT_FINISHED, "  "),
            new IsCompleteReply(Status.NOT_FINISHED, "   "),
            new IsCompleteReply(Status.NOT_FINISHED, "    "),
            new IsCompleteReply(Status.NOT_FINISHED, "     "),
            new IsCompleteReply(Status.NOT_FINISHED, "      "),
            new IsCompleteReply(Status.NOT_FINISHED, "       "),
            new IsCompleteReply(Status.NOT_FINISHED, "        "),
            new IsCompleteReply(Status.NOT_FINISHED, "\t"),
            new IsCompleteReply(Status.NOT_FINISHED, "\t\t")
    };

    /**
     * Try to resolve the indent to a common, shared instance, otherwise
     * create a new one. Since many indent replies will be a short sequence
     * or whitespace or an empty string we can cache some of these.
     *
     * @param indent the indent to suggest the frontend prefixes the next
     *               line with
     *
     * @return a reply describing the indent suggestion
     */
    public static IsCompleteReply getIncompleteReplyWithIndent(String indent) {
        switch (indent) {
            case "":
                return COMMON_INDENTS[0];
            case " ":
                return COMMON_INDENTS[1];
            case "  ":
                return COMMON_INDENTS[2];
            case "   ":
                return COMMON_INDENTS[3];
            case "    ":
                return COMMON_INDENTS[4];
            case "     ":
                return COMMON_INDENTS[5];
            case "      ":
                return COMMON_INDENTS[6];
            case "       ":
                return COMMON_INDENTS[7];
            case "        ":
                return COMMON_INDENTS[8];
            case "\t":
                return COMMON_INDENTS[9];
            case "\t\t":
                return COMMON_INDENTS[10];
            default:
                return new IsCompleteReply(Status.NOT_FINISHED, indent);
        }
    }

    public enum Status {
        @SerializedName("complete") VALID_CODE,
        @SerializedName("incomplete") NOT_FINISHED,
        @SerializedName("invalid") INVALID_CODE,
        @SerializedName("unknown") UNKNOWN
    }

    protected final Status status;

    /**
     * If status is INVALID_CODE this is a hint for the front end on what
     * to use for the indent on the next line.
     */
    protected final String indent;

    private IsCompleteReply(Status status) {
        this.status = status;
        this.indent = "";
    }

    private IsCompleteReply(Status status, String indent) {
        this.status = status;
        this.indent = indent;
    }

    public Status getStatus() {
        return status;
    }

    public String getIndent() {
        return indent;
    }
}
