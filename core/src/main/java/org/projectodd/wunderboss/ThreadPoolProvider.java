/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolProvider implements ComponentProvider<ThreadPool> {
    @Override
    public ThreadPool create(final String name, Options _) {
        //TODO: expose options?
        final ExecutorService executor =
                Executors.newCachedThreadPool(
                        new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable runnable) {
                                Thread t = new Thread(runnable);
                                t.setName(String.format("%s-%s",
                                                        name,
                                                        threadCounter.getAndIncrement()));

                                return t;
                            }

                            private final AtomicLong threadCounter = new AtomicLong(0);
                        }
                );

        return new ThreadPool() {
            @Override
            public void start() throws Exception {

            }

            @Override
            public void stop() throws Exception {
                shutdown();
            }

            @Override
            public boolean isRunning() {
                return !isShutdown();
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void shutdown() {
                executor.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return executor.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return executor.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return executor.isTerminated();
            }

            @Override
            public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
                return executor.awaitTermination(l, timeUnit);
            }

            @Override
            public <T> Future<T> submit(Callable<T> callable) {
                return executor.submit(callable);
            }

            @Override
            public <T> Future<T> submit(Runnable runnable, T t) {
                return executor.submit(runnable, t);
            }

            @Override
            public Future<?> submit(Runnable runnable) {
                return executor.submit(runnable);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
                return executor.invokeAll(collection);
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
                return executor.invokeAll(collection, l, timeUnit);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
                return executor.invokeAny(collection);
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                return executor.invokeAny(collection, l, timeUnit);
            }

            @Override
            public void execute(Runnable runnable) {
                executor.execute(runnable);
            }
        };
    }
}
