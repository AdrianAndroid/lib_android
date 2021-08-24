/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * RxJava2 interoperability Worker implementation.
 * <p>
 * When invoked by the {@link WorkManager}, it will call @{@link #createWork()} to get a
 * {@code Single<Result>} subscribe to it.
 * <p>
 * By default, RxWorker will subscribe on the thread pool that runs {@link WorkManager}
 * {@link Worker}s. You can change this behavior by overriding {@link #getBackgroundScheduler()}
 * method.
 * <p>
 * An RxWorker is given a maximum of ten minutes to finish its execution and return a
 * {@link androidx.work.ListenableWorker.Result}.  After this time has expired, the worker will be
 * signalled to stop.
 *
 * @see Worker
 */
public abstract class RxWorker extends ListenableWorker {
    @SuppressWarnings("WeakerAccess")
    static final Executor INSTANT_EXECUTOR = new SynchronousExecutor();

    @Nullable
    private SingleFutureAdapter<Result> mSingleFutureObserverAdapter;

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public RxWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        mSingleFutureObserverAdapter = new SingleFutureAdapter<>();

        final Scheduler scheduler = getBackgroundScheduler();
        createWork()
                .subscribeOn(scheduler)
                // observe on WM's private thread
                .observeOn(Schedulers.from(getTaskExecutor().getBackgroundExecutor()))
                .subscribe(mSingleFutureObserverAdapter);
        return mSingleFutureObserverAdapter.mFuture;
    }

    /**
     * Returns the default background scheduler that {@code RxWorker} will use to subscribe.
     * <p>
     * The default implementation returns a Scheduler that uses the {@link Executor} which was
     * provided in {@link WorkManager}'s {@link Configuration} (or the default one it creates).
     * <p>
     * You can override this method to change the Scheduler used by RxWorker to start its
     * subscription. It always observes the result of the {@link Single} in WorkManager's internal
     * thread.
     *
     * @return The default {@link Scheduler}.
     */
    protected @NonNull Scheduler getBackgroundScheduler() {
        return Schedulers.from(getBackgroundExecutor());
    }

    /**
     * Override this method to define your actual work and return a {@code Single} of
     * {@link androidx.work.ListenableWorker.Result} which will be subscribed by the
     * {@link WorkManager}.
     * <p>
     * If the returned {@code Single} fails, the worker will be considered as failed.
     * <p>
     * If the {@link RxWorker} is cancelled by the {@link WorkManager} (e.g. due to a constraint
     * change), {@link WorkManager} will dispose the subscription immediately.
     * <p>
     * By default, subscription happens on the shared {@link Worker} pool. You can change it
     * by overriding {@link #getBackgroundScheduler()}.
     * <p>
     * An RxWorker is given a maximum of ten minutes to finish its execution and return a
     * {@link androidx.work.ListenableWorker.Result}.  After this time has expired, the worker will
     * be signalled to stop.
     *
     * @return a {@code Single<Result>} that represents the work.
     */
    @MainThread
    public abstract @NonNull Single<Result> createWork();

    /**
     * Updates the progress for a {@link RxWorker}. This method returns a {@link Single} unlike the
     * {@link ListenableWorker#setProgressAsync(Data)} API.
     * <p>
     * This method is deprecated. Use {@link #setCompletableProgress(Data)} instead.
     *
     * @param data The progress {@link Data}
     * @return The {@link Single}
     * @deprecated This method is being deprecated because it is impossible to signal success via
     * a `Single&lt;Void&gt;` type. A {@link Completable} should have been used.
     * <p>
     * Use {@link #setCompletableProgress(Data)} instead.
     */
    @NonNull
    @Deprecated
    public final Single<Void> setProgress(@NonNull Data data) {
        return Single.fromFuture(setProgressAsync(data));
    }

    /**
     * Updates the progress for a {@link RxWorker}. This method returns a {@link Completable}
     * unlike the {@link ListenableWorker#setProgressAsync(Data)} API.
     *
     * @param data The progress {@link Data}
     * @return The {@link Completable}
     */
    @NonNull
    public final Completable setCompletableProgress(@NonNull Data data) {
        return Completable.fromFuture(setProgressAsync(data));
    }

    @Override
    public void onStopped() {
        super.onStopped();
        final SingleFutureAdapter<Result> observer = mSingleFutureObserverAdapter;
        if (observer != null) {
            observer.dispose();
            mSingleFutureObserverAdapter = null;
        }
    }

    /**
     * An observer that can observe a single and provide it as a {@link ListenableWorker}.
     */
    static class SingleFutureAdapter<T> implements SingleObserver<T>, Runnable {
        final SettableFuture<T> mFuture = SettableFuture.create();
        @Nullable
        private Disposable mDisposable;

        SingleFutureAdapter() {
            mFuture.addListener(this, INSTANT_EXECUTOR);
        }

        @Override
        public void onSubscribe(Disposable disposable) {
            mDisposable = disposable;
        }

        @Override
        public void onSuccess(T t) {
            mFuture.set(t);
        }

        @Override
        public void onError(Throwable throwable) {
            mFuture.setException(throwable);
        }

        @Override
        public void run() { // Future listener
            if (mFuture.isCancelled()) {
                dispose();
            }
        }

        void dispose() {
            final Disposable disposable = mDisposable;
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }
}
