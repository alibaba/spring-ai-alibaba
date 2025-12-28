/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.studio.core.utils.concurrent;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * A wrapper for ExecutorService that propagates and cleans up RequestContext across
 * thread boundaries. This ensures that request context information is properly maintained
 * in asynchronous operations.
 */
public class RequestContextThreadPoolWrapper implements ExecutorService {

	/** The underlying executor service being wrapped */
	private final ExecutorService delegate;

	/**
	 * Creates a new wrapper for the given executor service
	 * @param delegate the executor service to wrap
	 */
	public RequestContextThreadPoolWrapper(ExecutorService delegate) {
		this.delegate = delegate;
	}

	/**
	 * Executes the given command with request context propagation
	 */
	@Override
	public void execute(@NotNull Runnable command) {
		RequestContext context = RequestContextHolder.getRequestContext();
		delegate.execute(() -> {
			try {
				if (context != null) {
					RequestContextHolder.setRequestContext(context);
				}
				command.run();
			}
			finally {
				RequestContextHolder.clearRequestContext();
			}
		});
	}

	/**
	 * Submits a callable task with request context propagation
	 */
	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Callable<T> task) {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.submit(() -> {
			try {
				if (context != null) {
					RequestContextHolder.setRequestContext(context);
				}
				return task.call();
			}
			finally {
				RequestContextHolder.clearRequestContext();
			}
		});
	}

	/**
	 * Submits a runnable task with request context propagation and returns the given
	 * result
	 */
	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Runnable task, T result) {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.submit(() -> {
			try {
				if (context != null) {
					RequestContextHolder.setRequestContext(context);
				}
				task.run();
			}
			finally {
				RequestContextHolder.clearRequestContext();
			}
		}, result);
	}

	/**
	 * Submits a runnable task with request context propagation
	 */
	@NotNull
	@Override
	public Future<?> submit(@NotNull Runnable task) {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.submit(() -> {
			try {
				if (context != null) {
					RequestContextHolder.setRequestContext(context);
				}
				task.run();
			}
			finally {
				RequestContextHolder.clearRequestContext();
			}
		});
	}

	/**
	 * Invokes all tasks with request context propagation
	 */
	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.invokeAll(wrapCallables(tasks, context));
	}

	/**
	 * Invokes all tasks with request context propagation and timeout
	 */
	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout,
			@NotNull TimeUnit unit) throws InterruptedException {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.invokeAll(wrapCallables(tasks, context), timeout, unit);
	}

	/**
	 * Invokes any task with request context propagation
	 */
	@NotNull
	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.invokeAny(wrapCallables(tasks, context));
	}

	/**
	 * Invokes any task with request context propagation and timeout
	 */
	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		RequestContext context = RequestContextHolder.getRequestContext();
		return delegate.invokeAny(wrapCallables(tasks, context), timeout, unit);
	}

	/**
	 * Wraps a collection of callables with request context propagation
	 */
	private <T> Collection<? extends Callable<T>> wrapCallables(Collection<? extends Callable<T>> tasks,
			RequestContext context) {
		return tasks.stream().map(task -> (Callable<T>) () -> {
			try {
				if (context != null) {
					RequestContextHolder.setRequestContext(context);
				}
				return task.call();
			}
			finally {
				RequestContextHolder.clearRequestContext();
			}
		}).collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Initiates an orderly shutdown of the underlying executor
	 */
	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	/**
	 * Attempts to stop all actively executing tasks
	 */
	@NotNull
	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	/**
	 * Returns true if the executor has been shut down
	 */
	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	/**
	 * Returns true if all tasks have completed following shutdown
	 */
	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request
	 */
	@Override
	public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	/**
	 * Returns the underlying ThreadPoolExecutor if available
	 * @return the underlying ThreadPoolExecutor, or null if the delegate is not a
	 * ThreadPoolExecutor
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() {
		if (delegate instanceof ThreadPoolExecutor) {
			return (ThreadPoolExecutor) delegate;
		}
		return null;
	}

}
