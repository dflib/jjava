package org.dflib.jjava.kernel.execution;

import jdk.jshell.EvalException;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.SPIResolutionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ExecutionControl very similar to {@link jdk.jshell.execution.LocalExecutionControl} but which
 * also logs the actual result of an invocation before being serialized.
 */
public class JJavaExecutionControl extends DirectExecutionControl {

    // generate a semi-unique thread name prefix for each JVM run for easier detection of JJavaExecutionControl-produced threads
    private static final String THREAD_NAME_PREFIX = "jjava-exec-"
            + ThreadLocalRandom.current().ints(6, 'a', 'z' + 1).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            + "-";

    /**
     * A special "class name" for a {@link jdk.jshell.spi.ExecutionControl.UserException} such that it may be
     * identified after serialization into an {@link jdk.jshell.EvalException} via {@link
     * EvalException#getExceptionClassName()}.
     */
    // Has spaces to not collide with a class name
    public static final String EXECUTION_TIMEOUT_NAME = "Execution Timeout";

    /**
     * A special "class name" for a {@link jdk.jshell.spi.ExecutionControl.UserException} such that it may be
     * identified after serialization into an {@link jdk.jshell.EvalException} via {@link
     * EvalException#getExceptionClassName()}
     */
    public static final String EXECUTION_INTERRUPTED_NAME = "Execution Interrupted";

    private static final AtomicInteger EXECUTOR_THREAD_ID = new AtomicInteger(0);

    private final ExecutorService executor;
    private final long timeoutDuration;
    private final TimeUnit timeoutUnit;
    private final Map<String, Future<Object>> running;
    private final Map<String, Object> results;
    private final JJavaLoaderDelegate loaderDelegate;

    public JJavaExecutionControl(JJavaLoaderDelegate loaderDelegate, long timeoutDuration, TimeUnit timeoutUnit) {
        super(loaderDelegate);

        this.loaderDelegate = loaderDelegate;
        this.running = new ConcurrentHashMap<>();
        this.results = new ConcurrentHashMap<>();

        this.timeoutDuration = timeoutDuration;
        this.timeoutUnit = timeoutDuration > 0 ? Objects.requireNonNull(timeoutUnit) : TimeUnit.MILLISECONDS;
        this.executor = Executors.newCachedThreadPool(r -> new Thread(r, THREAD_NAME_PREFIX + EXECUTOR_THREAD_ID.getAndIncrement()));
    }

    /**
     * Returns JShell ClassLoader
     */
    public ClassLoader getClassLoader() {
        return loaderDelegate.getClassLoader();
    }

    public long getTimeoutDuration() {
        return timeoutDuration;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public Object takeResult(String id) {
        Object result = this.results.remove(id);
        if (result == null) {
            throw new IllegalStateException("No result with key: " + id);
        }

        return result;
    }

    public void unloadClass(String className) {
        loaderDelegate.unloadClass(className);
    }

    public void interrupt() {
        running.forEach((id, f) -> f.cancel(true));
    }

    /**
     * This method was hijacked and actually only returns a key that can be later retrieved via
     * {@link #takeResult(String)}. This should be called for every invocation as the objects are saved and not taking
     * them will leak the memory.
     *
     * @returns the key to use for {@link #takeResult(String) looking up the result}.
     */
    @Override
    protected String invoke(Method doitMethod) throws Exception {
        String id = UUID.randomUUID().toString();
        Object value = doInvoke(id, doitMethod);
        results.put(id, value);
        return id;
    }

    private Object doInvoke(String id, Method doitMethod) throws Exception {

        Future<Object> task = isNestedCall()
                // run on the same thread if the invocation is done within another invocation
                ? CompletableFuture.completedFuture(doitMethod.invoke(null))
                : executor.submit(() -> doitMethod.invoke(null));

        running.put(id, task);

        try {
            return timeoutDuration > 0 ? task.get(timeoutDuration, timeoutUnit) : task.get();
        } catch (CancellationException e) {
            // If canceled this means that stop() or interrupt() was invoked.
            if (executor.isShutdown()) {
                // If the executor is shutdown, the situation is the former in which
                // case the protocol is to throw an ExecutionControl.StoppedException.
                throw new StoppedException();
            } else {
                // The execution was purposely interrupted.
                throw new UserException("Execution interrupted.", EXECUTION_INTERRUPTED_NAME, e.getStackTrace());
            }
        } catch (ExecutionException e) {

            Throwable cause = e.getCause();
            if (cause instanceof InvocationTargetException) {
                cause = cause.getCause();
            }
            if (cause == null) {
                throw new UserException("null", "Unknown Invocation Exception", e.getStackTrace());
            } else if (cause instanceof SPIResolutionException) {
                throw new ResolutionException(((SPIResolutionException) cause).id(), cause.getStackTrace());
            } else {
                throw new UserException(String.valueOf(cause.getMessage()), cause.getClass().getName(), cause.getStackTrace());
            }
        } catch (TimeoutException e) {
            String message = String.format("Execution timed out after configured timeout of %d %s.",
                    timeoutDuration,
                    timeoutUnit.toString().toLowerCase());
            throw new UserException(message, EXECUTION_TIMEOUT_NAME, e.getStackTrace());
        } finally {
            running.remove(id, task);
        }
    }

    private boolean isNestedCall() {
        return Thread.currentThread().getName().startsWith(THREAD_NAME_PREFIX);
    }

    @Override
    public String toString() {
        return "JJavaExecutionControl{" +
                "timeoutTime=" + timeoutDuration +
                ", timeoutUnit=" + timeoutUnit +
                '}';
    }
}
