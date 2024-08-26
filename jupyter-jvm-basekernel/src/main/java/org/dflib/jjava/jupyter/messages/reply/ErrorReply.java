package org.dflib.jjava.jupyter.messages.reply;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ErrorReply implements ReplyType<Object> {
    @Override
    public MessageType<Object> getRequestType() {
        return MessageType.UNKNOWN;
    }

    public static ErrorReply of(Exception exception) {
        String name = exception.getClass().getSimpleName();
        String msg = exception.getLocalizedMessage();
        List<String> stacktrace = Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());

        return new ErrorReply(name, msg == null ? "" : msg, stacktrace);
    }

    protected final String status = "error";
    @SerializedName("ename")
    protected final String errName;
    @SerializedName("evalue")
    protected final String errMsg;
    @SerializedName("traceback")
    protected final List<String> stacktrace;

    //Present for the execute_reply in erroneous execution
    @SerializedName("execution_count")
    protected Integer count;

    public ErrorReply(String errName, String errMsg, List<String> stacktrace) {
        this.errName = errName;
        this.errMsg = errMsg;
        this.stacktrace = stacktrace;
    }

    public void setExecutionCount(int count) {
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorName() {
        return errName;
    }

    public String getErrorMessage() {
        return errMsg;
    }

    public List<String> getStacktrace() {
        return stacktrace;
    }
}
