package com.google.android.gms.tasks;
import java.util.concurrent.Executor;
/** Minimal GMS Task for headless harness (completed-immediately semantics). */
public abstract class Task<TResult> {
    public abstract boolean isComplete();
    public abstract boolean isSuccessful();
    public abstract boolean isCanceled();
    public abstract TResult getResult();
    public abstract Exception getException();
    public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> l) { if (l != null) l.onComplete(this); return this; }
    public Task<TResult> addOnCompleteListener(Executor e, OnCompleteListener<TResult> l) { return addOnCompleteListener(l); }
    public Task<TResult> addOnCompleteListener(android.app.Activity a, OnCompleteListener<TResult> l) { return addOnCompleteListener(l); }
    public Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> l) { if (l != null && isSuccessful()) l.onSuccess(getResult()); return this; }
    public Task<TResult> addOnSuccessListener(Executor e, OnSuccessListener<? super TResult> l) { return addOnSuccessListener(l); }
    public Task<TResult> addOnSuccessListener(android.app.Activity a, OnSuccessListener<? super TResult> l) { return addOnSuccessListener(l); }
    public Task<TResult> addOnFailureListener(OnFailureListener l) { if (l != null && !isSuccessful() && !isCanceled()) l.onFailure(getException()); return this; }
    public Task<TResult> addOnFailureListener(Executor e, OnFailureListener l) { return addOnFailureListener(l); }
    public Task<TResult> addOnFailureListener(android.app.Activity a, OnFailureListener l) { return addOnFailureListener(l); }
    public Task<TResult> addOnCanceledListener(OnCanceledListener l) { if (l != null && isCanceled()) l.onCanceled(); return this; }
    public Task<TResult> addOnCanceledListener(Executor e, OnCanceledListener l) { return addOnCanceledListener(l); }
    public <TContinuationResult> Task<TContinuationResult> continueWith(Continuation<TResult, TContinuationResult> c) {
        try { return Tasks.forResult(c.then(this)); } catch (Exception ex) { return Tasks.forException(ex); }
    }
    public <TContinuationResult> Task<TContinuationResult> continueWith(Executor e, Continuation<TResult, TContinuationResult> c) { return continueWith(c); }
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Continuation<TResult, Task<TContinuationResult>> c) {
        try { Task<TContinuationResult> t = c.then(this); return t != null ? t : Tasks.forResult(null); } catch (Exception ex) { return Tasks.forException(ex); }
    }
    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(SuccessContinuation<TResult, TContinuationResult> c) {
        if (!isSuccessful()) return Tasks.forException(getException() != null ? getException() : new RuntimeException("failed"));
        try { Task<TContinuationResult> t = c.then(getResult()); return t != null ? t : Tasks.forResult(null); } catch (Exception ex) { return Tasks.forException(ex); }
    }
}
