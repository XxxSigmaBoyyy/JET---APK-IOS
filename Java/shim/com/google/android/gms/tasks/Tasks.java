package com.google.android.gms.tasks;
import java.util.concurrent.*;
/** Factory for completed Tasks (harness). */
public final class Tasks {
    private Tasks() {}
    public static <TResult> Task<TResult> forResult(TResult result) { return new CompletedTask<>(result, null, false); }
    public static <TResult> Task<TResult> forException(Exception e) { return new CompletedTask<>(null, e != null ? e : new RuntimeException("error"), false); }
    public static <TResult> Task<TResult> forCanceled() { return new CompletedTask<>(null, null, true); }
    public static <TResult> TResult await(Task<TResult> task) throws ExecutionException, InterruptedException {
        if (task == null) throw new IllegalArgumentException("Task must not be null");
        if (!task.isComplete()) throw new IllegalStateException("Task not complete");
        if (task.isSuccessful()) return task.getResult();
        if (task.isCanceled()) throw new CancellationException("Task canceled");
        throw new ExecutionException(task.getException());
    }
    public static <TResult> TResult await(Task<TResult> task, long t, TimeUnit u) throws ExecutionException, InterruptedException, TimeoutException { return await(task); }
    public static Task<Void> whenAll(java.util.Collection<? extends Task<?>> tasks) { return forResult(null); }
    public static <TResult> Task<java.util.List<TResult>> whenAllSuccess(java.util.Collection<? extends Task<?>> tasks) { return forResult(java.util.Collections.emptyList()); }
    static final class CompletedTask<T> extends Task<T> {
        final T result; final Exception ex; final boolean canceled;
        CompletedTask(T r, Exception e, boolean c) { result = r; ex = e; canceled = c; }
        public boolean isComplete() { return true; }
        public boolean isSuccessful() { return !canceled && ex == null; }
        public boolean isCanceled() { return canceled; }
        public T getResult() { if (ex != null) throw new RuntimeException(ex); return result; }
        public Exception getException() { return ex; }
    }
}
