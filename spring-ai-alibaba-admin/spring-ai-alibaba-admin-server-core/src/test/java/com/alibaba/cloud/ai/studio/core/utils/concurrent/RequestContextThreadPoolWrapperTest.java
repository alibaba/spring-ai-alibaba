/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class RequestContextThreadPoolWrapperTest {

	@ParameterizedTest
	@ValueSource(strings = { "execute", "callable", "runnable", "result", "all", "any", "allTimed", "anyTimed" })
	void shouldRetainCallerContextWhenPoolIsSaturated(String operation) throws Exception {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(),
				new ThreadPoolExecutor.CallerRunsPolicy());
		RequestContextThreadPoolWrapper wrapper = new RequestContextThreadPoolWrapper(pool);
		CountDownLatch occupied = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		RequestContext context = new RequestContext();
		try {
			pool.submit(() -> {
				occupied.countDown();
				try {
					release.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertThat(occupied.await(5, TimeUnit.SECONDS)).isTrue();
			Thread caller = Thread.currentThread();
			RequestContextHolder.setRequestContext(context);
			Callable<String> task = () -> {
				assertThat(Thread.currentThread()).isSameAs(caller);
				assertThat(RequestContextHolder.getRequestContext()).isSameAs(context);
				return "done";
			};
			Runnable runnable = () -> {
				assertThat(Thread.currentThread()).isSameAs(caller);
				assertThat(RequestContextHolder.getRequestContext()).isSameAs(context);
			};
			switch (operation) {
				case "execute" -> wrapper.execute(runnable);
				case "callable" -> assertThat(wrapper.submit(task).get()).isEqualTo("done");
				case "runnable" -> wrapper.submit(runnable).get();
				case "result" -> assertThat(wrapper.submit(runnable, "done").get()).isEqualTo("done");
				case "all" -> assertThat(wrapper.invokeAll(List.of(task)).get(0).get()).isEqualTo("done");
				case "any" -> assertThat(wrapper.invokeAny(List.of(task))).isEqualTo("done");
				case "allTimed" ->
					assertThat(wrapper.invokeAll(List.of(task), 5, TimeUnit.SECONDS).get(0).get()).isEqualTo("done");
				case "anyTimed" -> assertThat(wrapper.invokeAny(List.of(task), 5, TimeUnit.SECONDS)).isEqualTo("done");
			}
			assertThat(RequestContextHolder.getRequestContext()).isSameAs(context);
		}
		finally {
			RequestContextHolder.clearRequestContext();
			release.countDown();
			pool.shutdownNow();
			assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	void shouldClearContextOnReusedWorker() throws Exception {
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newSingleThreadExecutor();
		RequestContextThreadPoolWrapper wrapper = new RequestContextThreadPoolWrapper(pool);
		RequestContext context = new RequestContext();
		try {
			RequestContextHolder.setRequestContext(context);
			assertThat(wrapper.submit(RequestContextHolder::getRequestContext).get()).isSameAs(context);
			RequestContextHolder.clearRequestContext();
			assertThat(pool.submit(RequestContextHolder::getRequestContext).get()).isNull();
			assertThat(wrapper.submit(RequestContextHolder::getRequestContext).get()).isNull();
		}
		finally {
			RequestContextHolder.clearRequestContext();
			pool.shutdownNow();
			assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		}
	}

}
