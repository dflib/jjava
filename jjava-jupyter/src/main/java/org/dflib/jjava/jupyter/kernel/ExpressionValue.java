package org.dflib.jjava.jupyter.kernel;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;

import java.util.List;

public abstract class ExpressionValue {

    private ExpressionValue() { } // Seal the class

    /**
     * Check if this {@link ExpressionValue} is a {@link Success Success}
     * or not (an {@link Error Error}. If this method returns {@code true}
     * then this object can be safely cast to a {@link Success} or if {@code false}
     * then {@link Error}.
     *
     * @return true if this values is an instance of {@link Success} and
     *         false if {@link Error}.
     */
    public abstract boolean isSuccess();

    public static class Error extends ExpressionValue {
        @SerializedName("ename")
        protected final String errName;
        @SerializedName("evalue")
        protected final String errMsg;
        @SerializedName("traceback")
        protected final List<String> stacktrace;

        public Error(String errName, String errMsg, List<String> stacktrace) {
            this.errName = errName;
            this.errMsg = errMsg;
            this.stacktrace = stacktrace;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        public String getErrName() {
            return this.errName;
        }

        public String getErrMsg() {
            return this.errMsg;
        }

        public List<String> getStacktrace() {
            return this.stacktrace;
        }
    }

    public static class Success extends ExpressionValue {
        protected final DisplayData data;

        public Success(DisplayData data) {
            this.data = data;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        public DisplayData getData() {
            return this.data;
        }
    }
}
