/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.controller.graph;

import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GraphProcessExceptionHandlingTest {

	@Mock
	private CompiledGraph compiledGraph;

	@Mock
	private AsyncGenerator<NodeOutput> generator;

	@Mock
	private NodeOutput nodeOutput;

	private GraphProcess graphProcess;

	private GraphId graphId;

	@BeforeEach
	void setUp() {
		graphProcess = new GraphProcess(compiledGraph);
		graphId = new GraphId("test-session", "test-graph-1");
	}

	@Test
	void testNodeExceptionHandling() throws Exception {
		RuntimeException testException = new RuntimeException("流程执行出错");

		CountDownLatch errorLatch = new CountDownLatch(1);
		CountDownLatch eventLatch = new CountDownLatch(1);

		AsyncGenerator.Data<NodeOutput> errorData = AsyncGenerator.Data.error(testException);
		when(generator.next()).thenReturn(errorData);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		AtomicBoolean errorEmitted = new AtomicBoolean(false);
		AtomicReference<Throwable> capturedError = new AtomicReference<>();

		sink.asFlux().subscribe(event -> {
			String data = event.data();
			if (data != null && data.contains("节点执行异常") && data.contains("流程执行出错")) {
				errorEmitted.set(true);
				eventLatch.countDown();
			}
		}, error -> {
			capturedError.set(error);
			errorLatch.countDown();
		});

		graphProcess.processStream(graphId, generator, sink);

		assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "应该在5秒内完成错误处理");
		assertTrue(eventLatch.await(5, TimeUnit.SECONDS), "应该在5秒内发送错误事件");

		assertTrue(errorEmitted.get(), "应该发送包含错误信息的事件");
		assertNotNull(capturedError.get(), "应该触发错误处理");
		assertTrue(capturedError.get() instanceof RuntimeException, "应该是 RuntimeException");
		assertEquals("流程执行出错", capturedError.get().getMessage(), "错误消息应该匹配");

		verify(generator, atLeastOnce()).next();
	}

	@Test
	void testNormalStreamProcessing() throws Exception {
		when(nodeOutput.node()).thenReturn("test-node");
		when(nodeOutput.state()).thenReturn(mock(OverAllState.class));
		
		AsyncGenerator.Data<NodeOutput> normalData = AsyncGenerator.Data.of(nodeOutput);
		AsyncGenerator.Data<NodeOutput> doneData = AsyncGenerator.Data.done();
		
		when(generator.next())
			.thenReturn(normalData)
			.thenReturn(doneData);
		
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		
		AtomicBoolean completed = new AtomicBoolean(false);
		
		sink.asFlux().subscribe(
			event -> {
			},
			error -> {
				fail("不应该有错误: " + error.getMessage());
			},
			() -> {
				completed.set(true);
			}
		);
		
		graphProcess.processStream(graphId, generator, sink);
		
		Thread.sleep(500);
		
		assertTrue(completed.get(), "流程应该正常完成");
		
		verify(generator, times(2)).next();
	}

	@Test
	void testCreateNewGraphId() {
		String sessionId = "test-session";

		GraphId graphId1 = graphProcess.createNewGraphId(sessionId);
		assertNotNull(graphId1);
		assertEquals(sessionId, graphId1.sessionId());
		assertTrue(graphId1.toString().contains(sessionId + "-"));

		GraphId graphId2 = graphProcess.createNewGraphId(sessionId);
		assertNotNull(graphId2);
		assertEquals(sessionId, graphId2.sessionId());
		assertNotEquals(graphId1.toString(), graphId2.toString());

		assertThrows(IllegalArgumentException.class, () -> {
			graphProcess.createNewGraphId("");
		});

		assertThrows(IllegalArgumentException.class, () -> {
			graphProcess.createNewGraphId(null);
		});
	}

	@Test
	void testStopGraph() throws Exception {
		CompletableFuture<NodeOutput> neverCompleteFuture = new CompletableFuture<>();
		AsyncGenerator.Data<NodeOutput> hangingData = AsyncGenerator.Data.of(neverCompleteFuture);
		when(generator.next()).thenReturn(hangingData);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// 监听任务启动
		sink.asFlux().subscribe(event -> {
			// 任务启动时会有事件
		}, error -> {
			// 错误处理
		}, () -> {
			// 完成处理
		});

		// 启动流程处理
		graphProcess.processStream(graphId, generator, sink);

		Thread.sleep(100);

		// 测试停止图执行
		boolean stopped = graphProcess.stopGraph(graphId);
		assertTrue(stopped, "应该能够停止正在运行的图任务");

		// 测试停止不存在的图
		GraphId nonExistentGraphId = new GraphId("non-existent", "non-existent-1");
		boolean notStopped = graphProcess.stopGraph(nonExistentGraphId);
		assertFalse(notStopped, "停止不存在的图应该返回false");
	}

}
