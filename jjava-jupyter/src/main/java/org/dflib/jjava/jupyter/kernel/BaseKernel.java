package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.ShellReplyEnvironment;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.common.Image;
import org.dflib.jjava.jupyter.kernel.display.common.Text;
import org.dflib.jjava.jupyter.kernel.display.common.Url;
import org.dflib.jjava.jupyter.kernel.history.HistoryEntry;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A common superclass of JVM-aware kernels.
 */
public abstract class BaseKernel {

    private final Logger LOGGER = LoggerFactory.getLogger("BaseKernel");

    // is only not null between "onStartup" and "onShutdown" of a singleton instance
    protected static BaseKernel notebookKernel;

    public static final String IS_COMPLETE_YES = "complete";
    public static final String IS_COMPLETE_BAD = "invalid";
    public static final String IS_COMPLETE_MAYBE = "unknown";

    // TODO: create some kind of metadata object to combine these properties?
    protected final String name;
    protected final String version;
    protected final LanguageInfo languageInfo;
    protected final List<HelpLink> helpLinks;

    protected final HistoryManager historyManager;
    protected final JupyterIO io;
    protected final CommManager commManager;
    protected final Renderer renderer;
    protected final MagicsResolver magicsResolver;
    protected final MagicsRegistry magicsRegistry;
    protected final Map<String, Extension> extensions;
    protected final boolean extensionsEnabled;
    protected final StringStyler errorStyler;
    protected final AtomicInteger executionCount;

    /**
     * Returns a non-null instance of the kernel associated with the current notebook. Throws an exception if called
     * outside the notebook lifecycle.
     */
    public static BaseKernel notebookKernel() {
        return Objects.requireNonNull(
                BaseKernel.notebookKernel,
                "No kernel running. Likely called outside of the notebook lifecycle");
    }

    protected BaseKernel(
            String name,
            String version,
            LanguageInfo languageInfo,
            List<HelpLink> helpLinks,
            HistoryManager historyManager,
            JupyterIO io,
            CommManager commManager,
            Renderer renderer,
            MagicsResolver magicsResolver,
            MagicsRegistry magicsRegistry,
            boolean extensionsEnabled,
            StringStyler errorStyler) {

        this.name = name;
        this.version = version;
        this.languageInfo = languageInfo;
        this.helpLinks = helpLinks;

        // allowed to be null
        this.historyManager = historyManager;

        this.io = Objects.requireNonNull(io);
        this.commManager = Objects.requireNonNull(commManager);
        this.renderer = Objects.requireNonNull(renderer);
        this.magicsResolver = magicsResolver;
        this.magicsRegistry = magicsRegistry;
        this.extensionsEnabled = extensionsEnabled;
        this.extensions = new ConcurrentHashMap<>();
        this.errorStyler = Objects.requireNonNull(errorStyler);

        this.executionCount = new AtomicInteger(1);

        Image.registerAll(this.renderer);
        Url.registerAll(this.renderer);
        Text.registerAll(this.renderer);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public MagicsRegistry getMagicsRegistry() {
        return magicsRegistry;
    }

    public MagicsResolver getMagicsResolver() {
        return magicsResolver;
    }

    public JupyterIO getIO() {
        return io;
    }

    public CommManager getCommManager() {
        return commManager;
    }

    public String getBanner() {
        return String.format("%s %s :: %s %s :: Protocol v%s",
                languageInfo != null ? languageInfo.getName() : "Unknown",
                languageInfo != null ? languageInfo.getVersion() : "",
                name != null ? name : "unknown",
                version != null ? version : "unknown",
                Header.PROTOCOL_VERISON
        );
    }

    public LanguageInfo getLanguageInfo() {
        return languageInfo;
    }

    public List<HelpLink> getHelpLinks() {
        return helpLinks;
    }

    /**
     * Get the active history manager for the kernel. If the history is ignored this method should return {@code null}.
     */
    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public void display(DisplayData data) {
        io.display.display(data);
    }

    /**
     * @deprecated unused, replaced with {@link #evalBuilder(String)} pipeline.
     */
    @Deprecated(forRemoval = true)
    public DisplayData eval(String source) {
        return evalBuilder(source).resolveMagics().renderResults().eval();
    }

    /**
     * @deprecated unused, replaced with {@link #evalBuilder(String)} pipeline.
     */
    @Deprecated(forRemoval = true)
    public Object evalRaw(String source) {
        return evalBuilder(source).resolveMagics().eval();
    }

    /**
     * Creates and returns a builder for an evaluation pipeline.
     */
    public <T> EvalBuilder<T> evalBuilder(String source) {
        return new SimpleEvalBuilder<>(this, source);
    }

    /**
     * Evaluates the source code in a way appropriate for a given kernel subclass.
     */
    protected abstract Object doEval(String source);

    /**
     * Inspect the code to get things such as documentation for a function. This is
     * triggered by {@code shift-tab} in the Jupyter notebook which opens a tooltip displaying
     * the returned bundle.
     * <p>
     * This should aim to return docstrings, function signatures, variable types, etc. for
     * the value at the cursor position.
     *
     * @param code        the entire code cell to inspect
     * @param at          the character position within the code cell
     * @param extraDetail true if more in depth detail is requested (for example IPython
     *                    includes the function source in addition to the documentation)
     * @return an output bundle for displaying the documentation or null if nothing is found
     * @throws RuntimeException if the code cannot be inspected for some reason (such as it not compiling)
     */
    public abstract DisplayData inspect(String code, int at, boolean extraDetail);

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
     * @return the replacement options containing a list of replacement texts and a
     * source range to overwrite with a user selected replacement from the list
     * @throws RuntimeException if code cannot be completed due to code compilation issues, or similar.
     *                          This should not be thrown if not replacements are available but rather just
     *                          an empty replacements returned.
     */
    public abstract ReplacementOptions complete(String code, int at);

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
     * @return {@link #IS_COMPLETE_MAYBE}, {@link #IS_COMPLETE_BAD}, {@link #IS_COMPLETE_YES},
     * or an indent string
     */
    public abstract String isComplete(String code);

    /**
     * Invoked after the kernel is created, but before it is returned to the environment. The default implementation
     * initializes static "notebookKernel" variable and loads extensions from the default classpath.
     */
    public void onStartup() {
        installNotebookKernel();

        if (extensionsEnabled) {
            installDefaultExtensions();
        }
    }

    /**
     * Invoked when the kernel is being shutdown. This is invoked before the connection is shutdown so any last minute
     * messages by the concrete kernel get a chance to send.
     *
     * @param isRestarting true if this is a shutdown will soon be followed by a restart. If running in a container or
     *                     some other spawned vm it may be beneficial to keep it alive for a bit longer as the kernel
     *                     is likely to be started up again.
     */
    public void onShutdown(boolean isRestarting) {
        uninstallExtension();
        uninstallNotebookKernel();
    }

    protected void uninstallExtension() {
        Set<Extension> localExts = new HashSet<>(extensions.values());
        extensions.clear();

        for (Extension ext : localExts) {
            try {
                ext.uninstall(this);
            } catch (Exception e) {
                LOGGER.info("Error uninstalling extension '{}', ignoring", ext.getClass().getName());
                LOGGER.debug("Uninstall error", e);
            }
        }
    }

    protected void installNotebookKernel() {
        if (BaseKernel.notebookKernel != null) {
            throw new IllegalStateException("A different notebook kernel was already started: " + BaseKernel.notebookKernel.getBanner());
        }

        BaseKernel.notebookKernel = this;
    }

    protected void uninstallNotebookKernel() {
        if (BaseKernel.notebookKernel != null && BaseKernel.notebookKernel != this) {
            throw new IllegalStateException("A different notebook kernel is running: " + BaseKernel.notebookKernel.getBanner());
        }

        BaseKernel.notebookKernel = null;
    }

    /**
     * Invoked when the kernel.json specifies an {@code interrupt_mode} of {@code message}
     * and the frontend requests an interrupt of the currently running cell.
     */
    public void interrupt() {
        // no-op
    }

    protected List<String> formatError(Throwable e) {
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

    public void becomeHandlerForConnection(JupyterConnection connection) {
        connection.setHandler(MessageType.EXECUTE_REQUEST, this::handleExecuteRequest);
        connection.setHandler(MessageType.INSPECT_REQUEST, this::handleInspectRequest);
        connection.setHandler(MessageType.COMPLETE_REQUEST, this::handleCompleteRequest);
        connection.setHandler(MessageType.HISTORY_REQUEST, this::handleHistoryRequest);
        connection.setHandler(MessageType.IS_COMPLETE_REQUEST, this::handleIsCodeCompeteRequest);
        connection.setHandler(MessageType.KERNEL_INFO_REQUEST, this::handleKernelInfoRequest);
        connection.setHandler(MessageType.SHUTDOWN_REQUEST, this::handleShutdownRequest);
        connection.setHandler(MessageType.INTERRUPT_REQUEST, this::handleInterruptRequest);

        commManager.setIOPubChannel(connection.getIOPub());
        connection.setHandler(MessageType.COMM_OPEN_COMMAND, commManager::handleCommOpenCommand);
        connection.setHandler(MessageType.COMM_MSG_COMMAND, commManager::handleCommMsgCommand);
        connection.setHandler(MessageType.COMM_CLOSE_COMMAND, commManager::handleCommCloseCommand);
        connection.setHandler(MessageType.COMM_INFO_REQUEST, commManager::handleCommInfoRequest);
    }

    protected void replaceOutputStreams(ShellReplyEnvironment env) {
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

    protected synchronized void handleExecuteRequest(ShellReplyEnvironment env, Message<ExecuteRequest> executeRequestMessage) {
        commManager.setMessageContext(executeRequestMessage);

        ExecuteRequest request = executeRequestMessage.getContent();

        int count = executionCount.getAndIncrement();

        env.setBusyDeferIdle();
        env.publish(new PublishExecuteInput(request.getCode(), count));

        replaceOutputStreams(env);

        io.setEnv(env);
        env.defer(() -> this.io.retractEnv(env));

        this.io.setJupyterInEnabled(request.isStdinEnabled());

        try {
            DisplayData out = evalBuilder(request.getCode()).resolveMagics().renderResults().eval();

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

    protected void handleInspectRequest(ShellReplyEnvironment env, Message<InspectRequest> inspectRequestMessage) {
        InspectRequest request = inspectRequestMessage.getContent();
        env.setBusyDeferIdle();
        try {
            DisplayData inspection = this.inspect(request.getCode(), request.getCursorPos(), request.getDetailLevel() > 0);
            env.reply(new InspectReply(inspection != null, DisplayData.emptyIfNull(inspection)));
        } catch (Exception e) {
            env.replyError(InspectReply.MESSAGE_TYPE.error(), ErrorReply.of(e));
        }
    }

    protected void handleCompleteRequest(ShellReplyEnvironment env, Message<CompleteRequest> completeRequestMessage) {
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

    protected void handleHistoryRequest(ShellReplyEnvironment env, Message<HistoryRequest> historyRequestMessage) {
        // If the manager is unset, short circuit and skip this message
        HistoryManager manager = this.getHistoryManager();
        if (manager == null) {
            return;
        }

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

        if (entries != null) {
            env.reply(new HistoryReply(entries));
        }
    }

    protected void handleIsCodeCompeteRequest(ShellReplyEnvironment env, Message<IsCompleteRequest> isCompleteRequestMessage) {
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

    protected void handleKernelInfoRequest(ShellReplyEnvironment env, Message<KernelInfoRequest> kernelInfoRequestMessage) {
        env.setBusyDeferIdle();
        env.reply(new KernelInfoReply(
                        Header.PROTOCOL_VERISON,
                        name,
                        version,
                        getLanguageInfo(),
                        getBanner(),
                        getHelpLinks()
                )
        );
    }

    protected void handleShutdownRequest(ShellReplyEnvironment env, Message<ShutdownRequest> shutdownRequestMessage) {
        ShutdownRequest request = shutdownRequestMessage.getContent();
        env.setBusyDeferIdle();

        env.defer().reply(request.isRestart() ? ShutdownReply.SHUTDOWN_AND_RESTART : ShutdownReply.SHUTDOWN);

        this.onShutdown(request.isRestart());

        env.resolveDeferrals(); //Resolve early because of shutdown

        env.markForShutdown();
    }

    protected void handleInterruptRequest(ShellReplyEnvironment env, Message<InterruptRequest> interruptRequestMessage) {
        env.setBusyDeferIdle();
        env.defer().reply(new InterruptReply());

        this.interrupt();
    }

    /**
     * Returns notebook ClassLoader.
     */
    protected ClassLoader getClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Locates, loads and initializes {@code Extension}s. Extension classes are discovered via {@link ServiceLoader},
     * using the kernel's default ClassLoader.
     */
    protected void installDefaultExtensions() {
        installExtensions(getClassLoader());
    }

    /**
     * Locates, loads and initializes {@code Extension}s. Extension classes are discovered via {@link ServiceLoader}.
     * It is passed a custom ClassLoader created internally based on a combination of the kernel ClassLoader
     * (see {@link #getClassLoader()}) and the extra classpath specified as an argument.
     *
     * @param classpath one or more filesystem paths separated by {@link java.io.File#pathSeparator}.
     */
    protected void installExtensions(String classpath) {

        URL[] urls = PathsHandler.split(classpath)
                .stream()
                .map(BaseKernel::pathToURL)
                .toArray(URL[]::new);

        URLClassLoader classLoader = new URLClassLoader(urls, getClassLoader());
        installExtensions(classLoader);
    }

    protected void installExtensions(ClassLoader classLoader) {
        ServiceLoader.load(Extension.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .forEach(this::installExtension);
    }

    protected void installExtension(Extension ext) {
        if (extensions.putIfAbsent(ext.getClass().getName(), ext) == null) {
            ext.install(this);
        }
    }

    private static URL pathToURL(String path) {
        try {
            return Path.of(path).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
