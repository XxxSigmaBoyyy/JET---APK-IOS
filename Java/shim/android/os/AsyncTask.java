package android.os;
import java.util.concurrent.*;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Host AsyncTask: runs {@link #doInBackground} on a worker pool and delivers
 * {@link #onPostExecute}/{@link #onProgressUpdate} on the main looper.
 * Replaces the auto-stub that left executeOnExecutor as a no-op (Signal
 * PassphraseCreateActivity SecretGenerator, Lottie-era loaders, etc.).
 */
public abstract class AsyncTask<Params, Progress, Result> {
    private static final int CORE = 1, MAX = 20;
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE, MAX, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(128),
            r -> { Thread t = new Thread(r, "AsyncTask"); t.setDaemon(true); return t; });
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
    private static volatile Executor sDefault = SERIAL_EXECUTOR;

    public enum Status { PENDING, RUNNING, FINISHED }

    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private volatile Status mStatus = Status.PENDING;
    private final FutureTask<Result> mFuture;
    private final WorkerRunnable mWorker;

    private abstract class WorkerRunnable implements Callable<Result> {
        Params[] mParams;
    }

    private static final class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
        Runnable mActive;
        public synchronized void execute(Runnable r) {
            mTasks.offer(() -> { try { r.run(); } finally { scheduleNext(); } });
            if (mActive == null) scheduleNext();
        }
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }

    public AsyncTask() {
        mWorker = new WorkerRunnable() {
            @Override public Result call() {
                mStatus = Status.RUNNING;
                Result r = null;
                try { r = doInBackground(mParams); }
                catch (Throwable t) { mCancelled.set(true); throw new RuntimeException(t); }
                finally { postResult(r); }
                return r;
            }
        };
        mFuture = new FutureTask<Result>(mWorker) {
            @Override protected void done() {
                try { get(); }
                catch (CancellationException e) { postResult(null); }
                catch (ExecutionException e) { /* already posted via cancelled path or ignored */ }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        };
    }

    private void postResult(Result result) {
        Handler h = mainHandler();
        h.post(() -> {
            if (mCancelled.get()) onCancelled(result);
            else onPostExecute(result);
            mStatus = Status.FINISHED;
        });
    }

    private static Handler mainHandler() {
        Looper main = Looper.getMainLooper();
        return main != null ? new Handler(main) : new Handler();
    }

    protected abstract Result doInBackground(Params... params);
    protected void onPreExecute() {}
    protected void onPostExecute(Result result) {}
    protected void onProgressUpdate(Progress... values) {}
    protected void onCancelled(Result result) { onCancelled(); }
    protected void onCancelled() {}

    protected final void publishProgress(Progress... values) {
        if (!mCancelled.get()) mainHandler().post(() -> onProgressUpdate(values));
    }

    public final boolean isCancelled() { return mCancelled.get(); }
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }
    public final Status getStatus() { return mStatus; }
    public final Result get() throws InterruptedException, ExecutionException { return mFuture.get(); }
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefault, params);
    }

    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        if (mStatus != Status.PENDING) {
            throw new IllegalStateException("Cannot execute task: the task is already " + mStatus);
        }
        mStatus = Status.RUNNING;
        onPreExecute();
        mWorker.mParams = params;
        exec.execute(mFuture);
        return this;
    }

    public static void execute(Runnable runnable) { sDefault.execute(runnable); }
    public static void setDefaultExecutor(Executor exec) { sDefault = exec; }
}
