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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ExecutionControl very similar to {@link jdk.jshell.execution.LocalExecutionControl} but which
 * also logs the actual result of an invocation before being serialized.
 */
public class JJavaExecutionControl extends DirectExecutionControl {
    /**
     * A special "class name" for a {@link jdk.jshell.spi.ExecutionControl.UserException} such that it may be
     * identified after serialization into an {@link jdk.jshell.EvalException} via {@link
     * EvalException#getExceptionClassName()}.
     */
    public static final String EXECUTION_TIMEOUT_NAME = "Execution Timeout"; // Has spaces to not collide with a class name

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
        this.executor = Executors.newCachedThreadPool(r -> new Thread(r, "JJava-executor-" + EXECUTOR_THREAD_ID.getAndIncrement()));
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
        Object value = execute(id, doitMethod);
        results.put(id, value);
        return id;
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public Object takeResult(String key) {
        Object result = this.results.remove(key);
        if (result == null) {
            throw new IllegalStateException("No result with key: " + key);
        }

        return result;
    }

    public void unloadClass(String className) {
        loaderDelegate.unloadClass(className);
    }

    public void interrupt() {
        running.forEach((id, f) -> f.cancel(true));
    }

    @Override
    public String toString() {
        return "JJavaExecutionControl{" +
                "timeoutTime=" + timeoutDuration +
                ", timeoutUnit=" + timeoutUnit +
                '}';
    }

    private Object execute(String key, Method doitMethod) throws Exception {

        Future<Object> runningTask = executor.submit(() -> doitMethod.invoke(null));
        running.put(key, runningTask);

        try {
            return timeoutDuration > 0 ? runningTask.get(this.timeoutDuration, this.timeoutUnit) : runningTask.get();
        } catch (CancellationException e) {
            // If canceled this means that stop() or interrupt() was invoked.
            if (executor.isShutdown()) {
                // If the executor is shutdown, the situation is the former in which
                // case the protocol is to throw an ExecutionControl.StoppedException.
                throw new StoppedException();
            } else {
                // The execution was purposely interrupted.
                throw new UserException(
                        "Execution interrupted.",
                        EXECUTION_INTERRUPTED_NAME,
                        e.getStackTrace());
            }
        } catch (ExecutionException e) {
            // The execution threw an exception. The actual exception is the cause of the ExecutionException.
            Throwable cause = e.getCause();
            if (cause instanceof InvocationTargetException) {
                // Unbox further
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
            throw new UserException(
                    String.format("Execution timed out after configured timeout of %d %s.", this.timeoutDuration, this.timeoutUnit.toString().toLowerCase()),
                    EXECUTION_TIMEOUT_NAME,
                    e.getStackTrace()
            );
        } finally {
            running.remove(key, runningTask);
        }
    }
}
