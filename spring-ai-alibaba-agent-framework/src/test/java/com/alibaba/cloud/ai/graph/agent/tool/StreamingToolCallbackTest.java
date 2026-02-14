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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for StreamingToolCallback interface.
 *
 * <p>
 * Covers streaming contract, async fallback, and merge behavior.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("StreamingToolCallback Tests")
class StreamingToolCallbackTest {

	@Nested
	@DisplayName("Basic Contract Tests")
	class BasicContractTests {

		@Test
		@DisplayName("isStreaming() should return true")
		void isStreaming_shouldReturnTrue() {
			StreamingToolCallback callback = createSimpleStreamingCallback();
			assertTrue(callback.isStreaming());
		}

		@Test
		@DisplayName("isAsync() should return true (inherited)")
		void isAsync_shouldReturnTrue() {
			StreamingToolCallback callback = createSimpleStreamingCallback();
			assertTrue(callback.isAsync());
		}

		@Test
		@DisplayName("callStream() should return Flux of ToolResult")
		void callStream_shouldReturnFlux() throws InterruptedException {
			StreamingToolCallback callback = createSimpleStreamingCallback();
			List<ToolResult> results = new ArrayList<>();
			CountDownLatch latch = new CountDownLatch(1);

			Flux<ToolResult> stream = callback.callStream("{}", new ToolContext(Map.of()));
			stream.doOnNext(results::add).doOnComplete(latch::countDown).subscribe();

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			assertEquals(3, results.size());
			assertEquals("chunk1", results.get(0).getTextContent());
			assertEquals("chunk2", results.get(1).getTextContent());
			assertEquals("chunk3", results.get(2).getTextContent());
			assertTrue(results.get(2).isFinal());
		}

	}

	@Nested
	@DisplayName("Async Fallback Tests")
	class AsyncFallbackTests {

		@Test
		@DisplayName("callAsync() should collect and merge all chunks")
		void callAsync_shouldCollectAndMergeChunks() {
			StreamingToolCallback callback = createSimpleStreamingCallback();

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));

			assertNotNull(future);
			String result = future.join();
			assertEquals("chunk1chunk2chunk3", result);
		}

		@Test
		@DisplayName("callAsync() should handle empty stream")
		void callAsync_shouldHandleEmptyStream() {
			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.empty();
				}
			};

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));
			String result = future.join();

			// Empty stream should result in empty or null string
			assertTrue(result == null || result.isEmpty());
		}

		@Test
		@DisplayName("callAsync() should propagate errors")
		void callAsync_shouldPropagateErrors() {
			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.error(new RuntimeException("Stream error"));
				}
			};

			CompletableFuture<String> future = callback.callAsync("{}", new ToolContext(Map.of()));

			assertTrue(future.isCompletedExceptionally());
		}

	}

	@Nested
	@DisplayName("Stream Behavior Tests")
	class StreamBehaviorTests {

		@Test
		@DisplayName("stream should emit multiple chunks")
		void stream_shouldEmitMultipleChunks() throws InterruptedException {
			AtomicInteger emitCount = new AtomicInteger(0);
			List<ToolResult> received = new ArrayList<>();
			CountDownLatch latch = new CountDownLatch(1);

			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.range(1, 10).map(i -> {
						emitCount.incrementAndGet();
						return ToolResult.chunk("chunk" + i);
					});
				}
			};

			Flux<ToolResult> stream = callback.callStream("{}", new ToolContext(Map.of()));
			stream.doOnNext(received::add).doOnComplete(latch::countDown).subscribe();

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			assertEquals(10, received.size());
			assertEquals(10, emitCount.get());
		}

		@Test
		@DisplayName("stream should support take operation")
		void stream_shouldSupportTakeOperation() throws InterruptedException {
			List<ToolResult> received = new ArrayList<>();
			CountDownLatch latch = new CountDownLatch(1);

			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.range(1, 100).map(i -> ToolResult.chunk("chunk" + i));
				}
			};

			Flux<ToolResult> stream = callback.callStream("{}", new ToolContext(Map.of()));
			stream.take(5).doOnNext(received::add).doOnComplete(latch::countDown).subscribe();

			assertTrue(latch.await(5, TimeUnit.SECONDS));
			assertEquals(5, received.size());
		}

	}

	@Nested
	@DisplayName("Merge Reduce Tests")
	class MergeReduceTests {

		@Test
		@DisplayName("merge should accumulate text correctly")
		void merge_shouldAccumulateText() {
			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(ToolResult.chunk("Hello, "), ToolResult.chunk("World"), ToolResult.finalChunk("!"));
				}
			};

			String result = callback.callAsync("{}", new ToolContext(Map.of())).join();

			assertEquals("Hello, World!", result);
		}

		@Test
		@DisplayName("merge should preserve final flag")
		void merge_shouldPreserveFinalFlag() {
			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.just(ToolResult.chunk("Part1"), ToolResult.finalChunk("Part2"));
				}
			};

			Flux<ToolResult> stream = callback.callStream("{}", new ToolContext(Map.of()));

			// Reduce manually to verify final flag
			ToolResult reduced = stream.reduce(ToolResult::merge).block();

			assertNotNull(reduced);
			assertTrue(reduced.isFinal());
			assertEquals("Part1Part2", reduced.getTextContent());
		}

	}

	@Nested
	@DisplayName("Error Handling Tests")
	class ErrorHandlingTests {

		@Test
		@DisplayName("stream error should not prevent partial processing")
		void streamError_shouldNotPreventPartialProcessing() throws InterruptedException {
			List<String> processedChunks = new ArrayList<>();
			CountDownLatch errorLatch = new CountDownLatch(1);

			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Flux<ToolResult> callStream(String arguments, ToolContext context) {
					return Flux.concat(Flux.just(ToolResult.chunk("good1"), ToolResult.chunk("good2")),
							Flux.error(new RuntimeException("Error after good chunks")));
				}
			};

			Flux<ToolResult> stream = callback.callStream("{}", new ToolContext(Map.of()));
			stream.doOnNext(r -> processedChunks.add(r.getTextContent()))
				.doOnError(ex -> errorLatch.countDown())
				.subscribe();

			assertTrue(errorLatch.await(5, TimeUnit.SECONDS));
			assertEquals(2, processedChunks.size());
		}

	}

	@Nested
	@DisplayName("Timeout Tests")
	class TimeoutTests {

		@Test
		@DisplayName("getTimeout() should return default timeout")
		void getTimeout_shouldReturnDefault() {
			StreamingToolCallback callback = createSimpleStreamingCallback();
			assertEquals(Duration.ofMinutes(5), callback.getTimeout());
		}

		@Test
		@DisplayName("custom timeout should be respected")
		void customTimeout_shouldBeRespected() {
			StreamingToolCallback callback = new TestStreamingToolCallback() {
				@Override
				public Duration getTimeout() {
					return Duration.ofSeconds(30);
				}
			};

			assertEquals(Duration.ofSeconds(30), callback.getTimeout());
		}

	}

	@Nested
	@DisplayName("Sync Fallback Tests")
	class SyncFallbackTests {

		@Test
		@DisplayName("call() should block and return merged result")
		void call_shouldBlockAndReturnMergedResult() {
			StreamingToolCallback callback = createSimpleStreamingCallback();

			String result = callback.call("{}");

			assertEquals("chunk1chunk2chunk3", result);
		}

	}

	// Helper methods

	private StreamingToolCallback createSimpleStreamingCallback() {
		return new TestStreamingToolCallback();
	}

	/**
	 * Test implementation of StreamingToolCallback
	 */
	private static class TestStreamingToolCallback implements StreamingToolCallback {

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder().name("testStreamingTool").description("A test streaming tool").build();
		}

		@Override
		public Flux<ToolResult> callStream(String arguments, ToolContext context) {
			return Flux.just(ToolResult.chunk("chunk1"), ToolResult.chunk("chunk2"), ToolResult.finalChunk("chunk3"));
		}

		@Override
		public String call(String toolInput) {
			return callAsync(toolInput, new ToolContext(Map.of())).join();
		}

	}

}
