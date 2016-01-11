/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
  CompletableFuture is java 8+, so this is just enough of it to do what we need
  so we can continue to support java 7.
 */
public class CompletableFuture<V> implements Future<V> {
    @Override
    public boolean cancel(boolean b) {
         throw new UnsupportedOperationException("cancelling not implemented");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return this.complete.getCount() == 0;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        this.complete.await();
        throwIfCompletedExceptionally();

        return this.value;
    }

    @Override
    public V get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        this.complete.await(l, timeUnit);
        throwIfCompletedExceptionally();

        return this.value;
    }

    public synchronized void complete(V value) {
        throwIfDone();
        this.value = value;
        this.complete.countDown();
    }

    public synchronized void completeExceptionally(Throwable e) {
        throwIfDone();
        this.error = e;
        this.complete.countDown();
    }

    public synchronized boolean isCompletedExceptionally() {
        return isDone() && this.error != null;
    }

    private void throwIfDone() {
        if (isDone()) {
            throw new RuntimeException("future already complete");
        }
    }

    private void throwIfCompletedExceptionally() throws ExecutionException {
        if (isCompletedExceptionally()) {
            throw new ExecutionException("Future completed exceptionally", this.error);
        }
    }

    private volatile V value;
    private volatile Throwable error;
    private final CountDownLatch complete = new CountDownLatch(1);
}
