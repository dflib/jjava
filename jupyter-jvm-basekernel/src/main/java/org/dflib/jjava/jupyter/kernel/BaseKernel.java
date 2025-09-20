package org.dflib.jjava.jupyter.kernel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.channels.ShellReplyEnvironment;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.common.Image;
import org.dflib.jjava.jupyter.kernel.display.common.Text;
import org.dflib.jjava.jupyter.kernel.display.common.Url;
import org.dflib.jjava.jupyter.kernel.history.HistoryEntry;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.kernel.util.TextColor;
import org.dflib.jjava.jupyter.messages.Header;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.publish.PublishError;
import org.dflib.jjava.jupyter.messages.publish.PublishExecuteInput;
import org.dflib.jjava.jupyter.messages.publish.PublishExecuteResult;
import org.dflib.jjava.jupyter.messages.reply.CompleteReply;
import org.dflib.jjava.jupyter.messages.reply.ErrorReply;
import org.dflib.jjava.jupyter.messages.reply.ExecuteReply;
import org.dflib.jjava.jupyter.messages.reply.HistoryReply;
import org.dflib.jjava.jupyter.messages.reply.InspectReply;
import org.dflib.jjava.jupyter.messages.reply.InterruptReply;
import org.dflib.jjava.jupyter.messages.reply.IsCompleteReply;
import org.dflib.jjava.jupyter.messages.reply.KernelInfoReply;
import org.dflib.jjava.jupyter.messages.reply.ShutdownReply;
import org.dflib.jjava.jupyter.messages.request.CompleteRequest;
import org.dflib.jjava.jupyter.messages.request.ExecuteRequest;
import org.dflib.jjava.jupyter.messages.request.HistoryRequest;
import org.dflib.jjava.jupyter.messages.request.InspectRequest;
import org.dflib.jjava.jupyter.messages.request.InterruptRequest;
import org.dflib.jjava.jupyter.messages.request.IsCompleteRequest;
import org.dflib.jjava.jupyter.messages.request.KernelInfoRequest;
import org.dflib.jjava.jupyter.messages.request.ShutdownRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseKernel {
    protected final AtomicInteger executionCount = new AtomicInteger(1);
    protected static final Map<String, String> KERNEL_META;

    static {
        Map<String, String> meta = null;

        InputStream metaStream = BaseKernel.class.getClassLoader().getResourceAsStream("kernel-metadata.json");
        if (metaStream != null) {
            Reader metaReader = new InputStreamReader(metaStream);
            try {
                meta = new Gson().fromJson(metaReader, new TypeToken<Map<String, String>>() {
                }.getType());
            } finally {
                try {
                    metaReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (meta == null) {
            meta = new HashMap<>(2);
            meta.put("version", "unknown");
            meta.put("project", "unknown");
        }

        KERNEL_META = meta;
    }

    private final JupyterIO io;
    private boolean shouldReplaceStdStreams;

    protected CommManager commManager;

    protected Renderer renderer;

    protected StringStyler errorStyler;

    public BaseKernel(Charset charset) {
        this.io = new JupyterIO(charset);
        this.shouldReplaceStdStreams = true;

        this.commManager = new CommManager();

        this.renderer = new Renderer();
        Image.registerAll(this.renderer);
        Url.registerAll(this.renderer);
        Text.registerAll(this.renderer);

        this.errorStyler = new StringStyler.Builder()
                .addPrimaryStyle(TextColor.BOLD_RESET_FG)
                .addSecondaryStyle(TextColor.BOLD_RED_FG)
                .addHighlightStyle(TextColor.BOLD_RESET_FG)
                .addHighlightStyle(TextColor.RED_BG)
                .build();
    }

    public BaseKernel() {
        this(JupyterSocket.UTF_8);
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public void display(DisplayData data) {
        this.io.display.display(data);
    }

    public JupyterIO getIO() {
        return this.io;
    }

    public CommManager getCommManager() {
        return this.commManager;
    }

    public boolean shouldReplaceStdStreams() {
        return this.shouldReplaceStdStreams;
    }

    public void setShouldReplaceStdStreams(boolean shouldReplaceStdStreams) {
        this.shouldReplaceStdStreams = shouldReplaceStdStreams;
    }

    public String getBanner() {
        LanguageInfo info = this.getLanguageInfo();
        return info != null ? info.getName() + " - " + info.getVersion() : "";
    }

    public List<LanguageInfo.Help> getHelpLinks() {
        return null;
    }

    /**
     * Get the active history manager for the kernel. If the history is ignored this method
     * should return {@code null}.
     *
     * @return the active {@link HistoryManager} or {@code null}.
     */
    public HistoryManager getHistoryManager() {
        return null;
    }

    /**
     * Evaluates a code expression in the kernel's language environment and returns the result
     * as display data. This is the core evaluation method called when executing code cells
     * in a Jupyter notebook.
     *
     * <p>The implementation should:
     * <ul>
     *   <li>Parse and evaluate the provided expression string</li>
     *   <li>Convert the evaluation result into appropriate display data formats</li>
     *   <li>Handle any language-specific evaluation context/scope</li>
     * </ul>
     *
     * <p>The returned {@link DisplayData} can contain multiple representations of the result
     * (e.g. text/plain, text/html, image/png) to allow rich display in the notebook.
     * Return null if the expression produces no displayable result.
     *
     * @param expr The code expression to evaluate as received from the Jupyter frontend
     *
     * @return A {@link DisplayData} object containing the evaluation result in one or more
     *         MIME formats, or null if there is no displayable result
     */
    public abstract DisplayData eval(String expr);

    /**
     * Inspect the code to get things such as documentation for a function. This is
     * triggered by {@code shift-tab} in the Jupyter notebook which opens a tooltip displaying
     * the returned bundle.
     * <p>
     * This should aim to return docstrings, function signatures, variable types, etc for
     * the value at the cursor position.
     *
     * @param code        the entire code cell to inspect
     * @param at          the character position within the code cell
     * @param extraDetail true if more in depth detail is requested (for example IPython
     *                    includes the function source in addition to the documentation)
     *
     * @return an output bundle for displaying the documentation or null if nothing is found
     *
     * @throws RuntimeException if the code cannot be inspected for some reason (such as it not compiling)
     */
    public DisplayData inspect(String code, int at, boolean extraDetail) {
        return null;
    }

    /**
     * Try to autocomplete code at a user's cursor such as finishing a method call or
     * variable name. This is triggered by {@code tab} in the Jupyter notebook.
     * <p>
     * If a single value is returned the replacement range in the {@code code} is replaced
     * with the return value.
     * <p>
     * If multiple matches are returned, a tooltip with the values in the order they are
     * returned is displayed that can be selected from.
     * <p>
     * If no matches are returned, no replacements are made. Effectively this is a no-op
     * in that case.
     *
     * @param code the entire code cell containing the code to complete
     * @param at   the character position that the completion is requested at
     *
     * @return the replacement options containing a list of replacement texts and a
     *         source range to overwrite with a user selected replacement from the list
     *
     * @throws RuntimeException if code cannot be completed due to code compilation issues, or similar.
     *                          This should not be thrown if not replacements are available but rather just
     *                          an empty replacements returned.
     */
    public ReplacementOptions complete(String code, int at) {
        return null;
    }

    protected static final String IS_COMPLETE_YES = "complete";
    protected static final String IS_COMPLETE_BAD = "invalid";
    protected static final String IS_COMPLETE_MAYBE = "unknown";

    /**
     * Check if the code is complete. This gives frontends the tools to provide
     * console environments that hold of executing code in situations such as
     * {@code "for (int i = 0; i < 10; i++)"} and rather let the newline go to
     * the next line for the developer to input the body of the for loop.
     * <p>
     * There are 4 cases to consider:
     * <p>
     * 1. {@link #IS_COMPLETE_MAYBE} is returned by default and is the equivalent
     * of abstaining from answering the request. <br>
     * 2. {@link #IS_COMPLETE_BAD} should be returned for invalid code that will
     * result in an error when being parsed/compiled. <br>
     * 3. {@link #IS_COMPLETE_YES} if the code is a complete, well formed, statement
     * and may be executed. <br>
     * 4. The code is valid but not yet complete (like the for loop example above). In
     * this case a string describing the prefix to start the next line with (such as 4 spaces
     * following the for loop). <br>
     *
     * @param code the code to analyze
     *
     * @return {@link #IS_COMPLETE_MAYBE}, {@link #IS_COMPLETE_BAD}, {@link #IS_COMPLETE_YES},
     *         or an indent string
     */
    public String isComplete(String code) {
        return IS_COMPLETE_MAYBE;
    }

    public abstract LanguageInfo getLanguageInfo();

    /**
     * Invoked when the kernel is being shutdown. This is invoked before the
     * connection is shutdown so any last minute messages by the concrete
     * kernel get a chance to send.
     *
     * @param isRestarting true if this is a shutdown will soon be followed
     *                     by a restart. If running in a container or some other
     *                     spawned vm it may be beneficial to keep it alive for a
     *                     bit longer as the kernel is likely to be started up
     *                     again.
     */
    public void onShutdown(boolean isRestarting) {
        //no-op
    }

    /**
     * Invoked when the kernel.json specifies an {@code interrupt_mode} of {@code message}
     * and the frontend requests an interrupt of the currently running cell.
     */
    public void interrupt() {
        //no-op
    }

    /**
     * Formats an error into a human friendly format. The default implementation prints
     * the stack trace as written by {@link Throwable#printStackTrace()} with a dividing
     * separator as a prefix.
     * <p>
     * Subclasses may override this method write better messages for specific errors but
     * may choose to still use this to display the stack trace. In this case it is recommended
     * to add the output of this call to the end of the output list.
     *
     * @param e the error to format
     *
     * @return a list of lines that make up the formatted error. This format should
     *         not include strings with newlines but rather separate strings each to go on a
     *         new line.
     */
    public List<String> formatError(Throwable e) {
        List<String> lines = new ArrayList<>();
        lines.add(this.errorStyler.secondary("---------------------------------------------------------------------------"));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.close();

        String stackTrace = stringWriter.toString();
        lines.addAll(this.errorStyler.secondaryLines(stackTrace));

        return lines;
    }

    /*
     * ===================================
     * | Default handler implementations |
     * ===================================
     */

    public void becomeHandlerForConnection(JupyterConnection connection) {
        connection.setHandler(MessageType.EXECUTE_REQUEST, this::handleExecuteRequest);
        connection.setHandler(MessageType.INSPECT_REQUEST, this::handleInspectRequest);
        connection.setHandler(MessageType.COMPLETE_REQUEST, this::handleCompleteRequest);
        connection.setHandler(MessageType.HISTORY_REQUEST, this::handleHistoryRequest);
        connection.setHandler(MessageType.IS_COMPLETE_REQUEST, this::handleIsCodeCompeteRequest);
        connection.setHandler(MessageType.KERNEL_INFO_REQUEST, this::handleKernelInfoRequest);
        connection.setHandler(MessageType.SHUTDOWN_REQUEST, this::handleShutdownRequest);
        connection.setHandler(MessageType.INTERRUPT_REQUEST, this::handleInterruptRequest);

        this.commManager.setIOPubChannel(connection.getIOPub());
        connection.setHandler(MessageType.COMM_OPEN_COMMAND, commManager::handleCommOpenCommand);
        connection.setHandler(MessageType.COMM_MSG_COMMAND, commManager::handleCommMsgCommand);
        connection.setHandler(MessageType.COMM_CLOSE_COMMAND, commManager::handleCommCloseCommand);
        connection.setHandler(MessageType.COMM_INFO_REQUEST, commManager::handleCommInfoRequest);
    }

    private void replaceOutputStreams(ShellReplyEnvironment env) {
        PrintStream oldStdOut = System.out;
        PrintStream oldStdErr = System.err;
        InputStream oldStdIn = System.in;

        System.setOut(this.io.out);
        System.setErr(this.io.err);
        System.setIn(this.io.in);

        env.defer(() -> {
            System.setOut(oldStdOut);
            System.setErr(oldStdErr);
            System.setIn(oldStdIn);
        });
    }

    private synchronized void handleExecuteRequest(ShellReplyEnvironment env, Message<ExecuteRequest> executeRequestMessage) {
        this.commManager.setMessageContext(executeRequestMessage);

        ExecuteRequest request = executeRequestMessage.getContent();

        int count = executionCount.getAndIncrement();
        //KernelTimestamp start = KernelTimestamp.now();

        env.setBusyDeferIdle();

        env.publish(new PublishExecuteInput(request.getCode(), count));

        if (this.shouldReplaceStdStreams())
            this.replaceOutputStreams(env);

        this.io.setEnv(env);
        env.defer(() -> this.io.retractEnv(env));

        this.io.setJupyterInEnabled(request.isStdinEnabled());

        try {
            DisplayData out = eval(request.getCode());

            if (out != null) {
                PublishExecuteResult result = new PublishExecuteResult(count, out);
                env.publish(result);
            }

            env.defer().reply(new ExecuteReply(count, Collections.emptyMap()));
        } catch (Exception e) {
            ErrorReply error = ErrorReply.of(e);
            error.setExecutionCount(count);
            env.publish(PublishError.of(e, this::formatError));
            env.defer().replyError(ExecuteReply.MESSAGE_TYPE.error(), error);
        }
    }

    private void handleInspectRequest(ShellReplyEnvironment env, Message<InspectRequest> inspectRequestMessage) {
        InspectRequest request = inspectRequestMessage.getContent();
        env.setBusyDeferIdle();
        try {
            DisplayData inspection = this.inspect(request.getCode(), request.getCursorPos(), request.getDetailLevel() > 0);
            env.reply(new InspectReply(inspection != null, DisplayData.emptyIfNull(inspection)));
        } catch (Exception e) {
            env.replyError(InspectReply.MESSAGE_TYPE.error(), ErrorReply.of(e));
        }
    }

    private void handleCompleteRequest(ShellReplyEnvironment env, Message<CompleteRequest> completeRequestMessage) {
        CompleteRequest request = completeRequestMessage.getContent();
        env.setBusyDeferIdle();
        try {
            ReplacementOptions options = this.complete(request.getCode(), request.getCursorPos());
            if (options == null)
                env.reply(new CompleteReply(Collections.emptyList(), request.getCursorPos(), request.getCursorPos(), Collections.emptyMap()));
            else
                env.reply(new CompleteReply(options.getReplacements(), options.getSourceStart(), options.getSourceEnd(), Collections.emptyMap()));
        } catch (Exception e) {
            env.replyError(CompleteReply.MESSAGE_TYPE.error(), ErrorReply.of(e));
        }
    }

    private void handleHistoryRequest(ShellReplyEnvironment env, Message<HistoryRequest> historyRequestMessage) {
        // If the manager is unset, short circuit and skip this message
        HistoryManager manager = this.getHistoryManager();
        if (manager == null) return;

        HistoryRequest request = historyRequestMessage.getContent();
        env.setBusyDeferIdle();

        Set<HistoryManager.ResultFlag> flags = EnumSet.noneOf(HistoryManager.ResultFlag.class);
        if (request.includeOutput()) flags.add(HistoryManager.ResultFlag.INCLUDE_OUTPUT);
        if (!request.useRaw()) flags.add(HistoryManager.ResultFlag.TRANSFORMED_INPUT);

        List<HistoryEntry> entries = null;
        switch (request.getAccessType()) {
            case TAIL:
                HistoryRequest.Tail tailRequest = ((HistoryRequest.Tail) request);
                entries = manager.lookupTail(tailRequest.getMaxReturnLength(), flags);
                break;
            case RANGE:
                HistoryRequest.Range rangeRequest = ((HistoryRequest.Range) request);
                entries = manager.lookupRange(rangeRequest.getSessionIndex(), rangeRequest.getStart(), rangeRequest.getStop(), flags);
                break;
            case SEARCH:
                HistoryRequest.Search searchRequest = ((HistoryRequest.Search) request);
                if (searchRequest.filterUnique()) flags.add(HistoryManager.ResultFlag.UNIQUE);
                entries = manager.search(searchRequest.getPattern(), searchRequest.getMaxReturnLength(), flags);
                break;
        }

        if (entries != null)
            env.reply(new HistoryReply(entries));
    }

    private void handleIsCodeCompeteRequest(ShellReplyEnvironment env, Message<IsCompleteRequest> isCompleteRequestMessage) {
        IsCompleteRequest request = isCompleteRequestMessage.getContent();
        env.setBusyDeferIdle();

        String isCompleteResult = this.isComplete(request.getCode());

        IsCompleteReply reply;
        switch (isCompleteResult) {
            case IS_COMPLETE_YES:
                reply = IsCompleteReply.VALID_CODE;
                break;
            case IS_COMPLETE_BAD:
                reply = IsCompleteReply.INVALID_CODE;
                break;
            case IS_COMPLETE_MAYBE:
                reply = IsCompleteReply.UNKNOWN;
                break;
            default:
                reply = IsCompleteReply.getIncompleteReplyWithIndent(isCompleteResult);
                break;
        }
        env.reply(reply);
    }

    private void handleKernelInfoRequest(ShellReplyEnvironment env, Message<KernelInfoRequest> kernelInfoRequestMessage) {
        env.setBusyDeferIdle();
        env.reply(new KernelInfoReply(
                        Header.PROTOCOL_VERISON,
                        KERNEL_META.get("project"),
                        KERNEL_META.get("version"),
                        this.getLanguageInfo(),
                        this.getBanner(),
                        this.getHelpLinks()
                )
        );
    }

    private void handleShutdownRequest(ShellReplyEnvironment env, Message<ShutdownRequest> shutdownRequestMessage) {
        ShutdownRequest request = shutdownRequestMessage.getContent();
        env.setBusyDeferIdle();

        env.defer().reply(request.isRestart() ? ShutdownReply.SHUTDOWN_AND_RESTART : ShutdownReply.SHUTDOWN);

        this.onShutdown(request.isRestart());

        env.resolveDeferrals(); //Resolve early because of shutdown

        env.markForShutdown();
    }

    private void handleInterruptRequest(ShellReplyEnvironment env, Message<InterruptRequest> interruptRequestMessage) {
        env.setBusyDeferIdle();
        env.defer().reply(new InterruptReply());

        this.interrupt();
    }
}
