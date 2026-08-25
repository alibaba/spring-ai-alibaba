/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.core.route;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.web.servlet.function.ServerResponse;

import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AErrorResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.spec.InternalError;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class A2aSseResponseWriterTest {

	private final ServerResponse.SseBuilder builder = mock(ServerResponse.SseBuilder.class);

	private final Logger logger = mock(Logger.class);

	private final A2AResponse<?> response = new A2AErrorResponse(new InternalError("expected"));

	@Test
	void naturalCompletionCompletesExactlyOnce() throws Exception {
		A2aSseResponseWriter.write(Flux.just(this.response), this.builder, this.logger, ignored -> "payload");

		verify(this.builder).data("payload");
		verify(this.builder, times(1)).complete();
		verify(this.builder, never()).error(any());
	}

	@Test
	void upstreamErrorSignalsErrorWithoutCompletion() {
		IllegalStateException failure = new IllegalStateException("boom");

		A2aSseResponseWriter.write(Flux.error(failure), this.builder, this.logger, ignored -> "payload");

		verify(this.builder, times(1)).error(failure);
		verify(this.builder, never()).complete();
	}

	@Test
	void serializationErrorCancelsTheSequenceAndSignalsOneError() throws Exception {
		AtomicInteger serialized = new AtomicInteger();

		A2aSseResponseWriter.write(Flux.just(this.response, this.response), this.builder, this.logger, ignored -> {
			serialized.incrementAndGet();
			throw new IllegalArgumentException("cannot serialize");
		});

		verify(this.builder, times(1)).error(any(IllegalArgumentException.class));
		verify(this.builder, never()).complete();
		verify(this.builder, never()).data(any());
		org.assertj.core.api.Assertions.assertThat(serialized).hasValue(1);
	}

	@Test
	void timeoutCancelsUpstreamAndCompletesExactlyOnce() {
		AtomicBoolean cancelled = new AtomicBoolean();
		ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);

		A2aSseResponseWriter.write(Flux.never().doOnCancel(() -> cancelled.set(true)), this.builder, this.logger,
				ignored -> "payload");
		verify(this.builder).onTimeout(timeout.capture());

		timeout.getValue().run();
		timeout.getValue().run();

		org.assertj.core.api.Assertions.assertThat(cancelled).isTrue();
		verify(this.builder, times(1)).complete();
		verify(this.builder, never()).error(any());
	}

	@Test
	void timeoutCancelsSynchronousSourceBeforeSubscribeReturns() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch cancelled = new CountDownLatch(1);
		CountDownLatch releaseSource = new CountDownLatch(1);
		ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
		Flux<Object> source = Flux.create(sink -> {
			sink.onCancel(() -> {
				cancelled.countDown();
				releaseSource.countDown();
			});
			subscribed.countDown();
			try {
				releaseSource.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = new Thread(() -> {
			try {
				A2aSseResponseWriter.write(source, this.builder, this.logger, ignored -> "payload");
			}
			catch (Throwable ex) {
				failure.set(ex);
			}
		});
		try {
			worker.start();
			org.assertj.core.api.Assertions.assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
			verify(this.builder).onTimeout(timeout.capture());

			timeout.getValue().run();
			worker.join(Duration.ofSeconds(2).toMillis());

			org.assertj.core.api.Assertions.assertThat(worker.isAlive()).isFalse();
			org.assertj.core.api.Assertions.assertThat(cancelled.await(2, TimeUnit.SECONDS)).isTrue();
			org.assertj.core.api.Assertions.assertThat(failure.get()).isNull();
			verify(this.builder, times(1)).complete();
			verify(this.builder, never()).error(any());
		}
		finally {
			releaseSource.countDown();
			worker.interrupt();
			worker.join(Duration.ofSeconds(2).toMillis());
		}
	}

	@Test
	void timeoutBeforeSubscriptionPreventsAnyUpstreamWork() throws Exception {
		AtomicBoolean subscribed = new AtomicBoolean();
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return this.builder;
		}).when(this.builder).onTimeout(any(Runnable.class));

		A2aSseResponseWriter.write(Flux.just(this.response).doOnSubscribe(ignored -> subscribed.set(true)),
				this.builder, this.logger, ignored -> "payload");

		org.assertj.core.api.Assertions.assertThat(subscribed).isFalse();
		verify(this.builder, times(1)).complete();
		verify(this.builder, never()).data(any());
		verify(this.builder, never()).error(any());
	}

}
