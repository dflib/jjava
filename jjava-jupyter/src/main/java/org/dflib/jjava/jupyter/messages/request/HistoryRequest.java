package org.dflib.jjava.jupyter.messages.request;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.ContentType;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.RequestType;
import org.dflib.jjava.jupyter.messages.reply.HistoryReply;

public class HistoryRequest implements ContentType<HistoryRequest>, RequestType<HistoryReply> {
    public static final MessageType<HistoryRequest> MESSAGE_TYPE = MessageType.HISTORY_REQUEST;
    public static final MessageType<HistoryReply> REPLY_MESSAGE_TYPE = MessageType.HISTORY_REPLY;

    @Override
    public MessageType<HistoryRequest> getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public MessageType<HistoryReply> getReplyType() {
        return REPLY_MESSAGE_TYPE;
    }

    public enum AccessType {
        @SerializedName("range") RANGE,
        @SerializedName("tail") TAIL,
        @SerializedName("search") SEARCH,
    }

    /**
     * If true, include the output associated with the inputs.
     */
    protected final boolean output;

    /**
     * If true, return the raw input history, else the transformed input.
     */
    protected final boolean raw;

    @SerializedName("hist_access_type")
    protected final AccessType accessType;

    private HistoryRequest(boolean output, boolean raw, AccessType accessType) {
        this.output = output;
        this.raw = raw;
        this.accessType = accessType;
    }

    public boolean includeOutput() {
        return output;
    }

    public boolean useRaw() {
        return raw;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public static class Range extends HistoryRequest {
        /**
         * A session index that counts up each time the kernel
         * starts. If negative the number is counting back from
         * the current session.
         */
        protected final int session;

        /**
         * Start cell (execution count number) within the session.
         */
        protected final int start;

        /**
         * Stop cell (execution count number) with the session.
         */
        protected final int stop;

        public Range(boolean output, boolean raw, int session, int start, int stop) {
            super(output, raw, AccessType.RANGE);
            this.session = session;
            this.start = start;
            this.stop = stop;
        }

        public int getSessionIndex() {
            return session;
        }

        public int getStart() {
            return start;
        }

        public int getStop() {
            return stop;
        }
    }

    public static class Tail extends HistoryRequest {
        /**
         * Get the last n executions
         */
        protected final int n;

        public Tail(boolean output, boolean raw, int n) {
            super(output, raw, AccessType.TAIL);
            this.n = n;
        }

        public int getMaxReturnLength() {
            return n;
        }
    }

    public static class Search extends HistoryRequest {
        /**
         * Get the last n executions
         */
        protected final int n;

        /**
         * Glob primary filter with '*' and '?'. Default to '*'
         */
        protected final String pattern;

        /**
         * If true, omit duplicate entries in the return. Defaults
         * to false.
         */
        protected final boolean unique;

        public Search(boolean output, boolean raw, int n, String pattern, boolean unique) {
            super(output, raw, AccessType.SEARCH);
            this.n = n;
            this.pattern = pattern;
            this.unique = unique;
        }

        public int getMaxReturnLength() {
            return n;
        }

        public String getPattern() {
            return pattern;
        }

        public boolean filterUnique() {
            return unique;
        }
    }
}
