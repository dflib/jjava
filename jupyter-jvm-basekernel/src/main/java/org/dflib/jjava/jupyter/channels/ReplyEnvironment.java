package org.dflib.jjava.jupyter.channels;

import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.reply.ErrorReply;

public interface ReplyEnvironment {
    void publish(Message<?> msg);

    void reply(Message<?> msg);

    /**
     * Defer the next message send until {@link #resolveDeferrals()}. Deferrals
     * are resolve in a Last In First Out (LIFO) order.
     * <p>
     * The use case that inspired this functionality is the busy-idle protocol
     * component required by Jupyter.
     *
     * <pre>
     *      ShellReplyEnvironment env = ...;
     *
     *      env.setStatusBusy();
     *      env.defer().setStatusIdle(); //Push idle message to defer stack
     *
     *      env.defer().reply(new ExecuteReply(...)); //Push reply to stack
     *
     *      env.writeToStdOut("Test"); //Write "Test" to std out now
     *
     *      env.resolveDeferrals();
     *      //Send the reply
     *      //Send the idle message
     * </pre>
     *
     * @return this instance for call chaining
     */
    ReplyEnvironment defer();

    /**
     * Defer an arbitrary action. See {@link #defer()} but instead of
     * deferring the next message send, defer a specific action.
     *
     * @param action the action to run when the deferrals are resolved
     */
    void defer(Runnable action);

    void resolveDeferrals();

    <T extends ContentType<T>> void publish(T content);

    <T extends ContentType<T>> void reply(T content);

    void replyError(MessageType<?> type, ErrorReply error);

    void setStatusBusy();

    void setStatusIdle();

    void setBusyDeferIdle();
}
