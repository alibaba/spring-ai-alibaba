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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.springframework.web.servlet.function.ServerResponse;

import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

final class A2aSseResponseWriter {

	private A2aSseResponseWriter() {
	}

	static void write(Flux<?> source, ServerResponse.SseBuilder builder, Logger logger) {
		write(source, builder, logger, A2aSseResponseWriter::serialize);
	}

	static void write(Flux<?> source, ServerResponse.SseBuilder builder, Logger logger,
			Function<A2AResponse<?>, String> serializer) {
		Object lifecycleLock = new Object();
		AtomicBoolean terminated = new AtomicBoolean();
		BaseSubscriber<String> subscriber = new BaseSubscriber<>() {
			@Override
			protected void hookOnSubscribe(Subscription subscription) {
				requestUnbounded();
			}

			@Override
			protected void hookOnNext(String data) {
				RuntimeException failure = null;
				synchronized (lifecycleLock) {
					if (terminated.get()) {
						return;
					}
					try {
							send(builder, data);
					}
					catch (RuntimeException ex) {
						terminated.set(true);
						failure = ex;
					}
				}
				if (failure != null) {
					cancel();
					logger.error("Agent SSE stream failed", failure);
					builder.error(failure);
				}
			}

			@Override
			protected void hookOnError(Throwable error) {
				if (claimTermination(lifecycleLock, terminated)) {
					logger.error("Agent SSE stream failed", error);
					builder.error(error);
				}
			}

			@Override
			protected void hookOnComplete() {
				if (claimTermination(lifecycleLock, terminated)) {
					builder.complete();
				}
			}
		};
		builder.onTimeout(() -> {
			if (claimTermination(lifecycleLock, terminated)) {
				subscriber.dispose();
				logger.debug("Agent SSE connection timed out.");
				builder.complete();
			}
		});
		builder.onError(error -> {
			boolean active;
			synchronized (lifecycleLock) {
				active = !terminated.get();
				terminated.set(true);
			}
			if (active) {
				subscriber.dispose();
			}
		});
		builder.onComplete(() -> {
			boolean active;
			synchronized (lifecycleLock) {
				active = !terminated.get();
				terminated.set(true);
			}
			if (active) {
				subscriber.dispose();
				logger.debug("Agent SSE connection completed.");
			}
		});

		Flux<A2AResponse<?>> responses = source.handle((value, sink) -> {
			if (value instanceof A2AResponse<?> response) {
				sink.next(response);
			}
			else {
				sink.error(new IllegalStateException("Unexpected A2A SSE response type: " + value));
			}
		});
		if (terminated.get()) {
			return;
		}
		responses.takeUntil(A2aSseResponseWriter::isFinalStatus).map(serializer).subscribe(subscriber);
	}

	private static boolean claimTermination(Object lifecycleLock, AtomicBoolean terminated) {
		synchronized (lifecycleLock) {
			return terminated.compareAndSet(false, true);
		}
	}

	private static boolean isFinalStatus(A2AResponse<?> response) {
		return response.getResult() instanceof TaskStatusUpdateEvent event && event.isFinal();
	}

	private static String serialize(A2AResponse<?> response) {
		try {
			return JsonUtil.toJson(response);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize A2A SSE response", ex);
		}
	}

	private static void send(ServerResponse.SseBuilder builder, String data) {
		try {
			builder.data(data);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write A2A SSE response", ex);
		}
	}

}
