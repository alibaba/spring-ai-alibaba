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
		// 准备测试数据
		RuntimeException testException = new RuntimeException("流程执行出错");

		// 创建一个包含异常的 CompletableFuture
		CompletableFuture<NodeOutput> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(testException);

		// 创建错误状态的 Data
		AsyncGenerator.Data<NodeOutput> errorData = AsyncGenerator.Data.error(testException);

		// 模拟 generator.next() 返回错误数据
		when(generator.next()).thenReturn(errorData);

		// 创建 Sink 来捕获事件
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// 用来捕获异常的标志
		AtomicBoolean errorEmitted = new AtomicBoolean(false);
		AtomicReference<Throwable> capturedError = new AtomicReference<>();

		// 订阅 sink 来捕获事件和错误
		sink.asFlux().subscribe(event -> {
			// 验证错误事件包含正确的错误信息
			String data = event.data();
			if (data != null && data.contains("节点执行异常") && data.contains("流程执行出错")) {
				errorEmitted.set(true);
			}
		}, error -> {
			capturedError.set(error);
		});

		// 执行测试
		graphProcess.processStream(graphId, generator, sink);

		// 等待异步处理完成
		Thread.sleep(500);

		// 验证错误被正确处理
		assertTrue(errorEmitted.get(), "应该发送包含错误信息的事件");
		assertNotNull(capturedError.get(), "应该触发错误处理");
		assertTrue(capturedError.get() instanceof RuntimeException, "应该是 RuntimeException");
		assertEquals("流程执行出错", capturedError.get().getMessage(), "错误消息应该匹配");

		// 验证 generator.next() 被调用了
		verify(generator, atLeastOnce()).next();
	}

	@Test
	void testNormalStreamProcessing() throws Exception {
		// 准备测试数据
		when(nodeOutput.node()).thenReturn("test-node");
		when(nodeOutput.state()).thenReturn(mock(OverAllState.class));
		
		// 创建正常的 Data
		AsyncGenerator.Data<NodeOutput> normalData = AsyncGenerator.Data.of(nodeOutput);
		AsyncGenerator.Data<NodeOutput> doneData = AsyncGenerator.Data.done();
		
		// 模拟 generator.next() 先返回正常数据，然后返回完成标志
		when(generator.next())
			.thenReturn(normalData)
			.thenReturn(doneData);
		
		// 创建 Sink 来捕获事件
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		
		// 用来捕获完成状态
		AtomicBoolean completed = new AtomicBoolean(false);
		
		// 订阅 sink 来验证完成状态
		sink.asFlux().subscribe(
			event -> {
				// 正常数据处理
			},
			error -> {
				fail("不应该有错误: " + error.getMessage());
			},
			() -> {
				completed.set(true);
			}
		);
		
		// 执行测试
		graphProcess.processStream(graphId, generator, sink);
		
		// 等待异步处理完成
		Thread.sleep(500);
		
		// 验证完成状态
		assertTrue(completed.get(), "流程应该正常完成");
		
		// 验证 generator.next() 被调用了两次
		verify(generator, times(2)).next();
	}

	@Test
	void testCreateNewGraphId() {
		String sessionId = "test-session";

		// 测试正常创建
		GraphId graphId1 = graphProcess.createNewGraphId(sessionId);
		assertNotNull(graphId1);
		assertEquals(sessionId, graphId1.sessionId());
		assertTrue(graphId1.toString().contains(sessionId + "-"));

		// 测试递增计数
		GraphId graphId2 = graphProcess.createNewGraphId(sessionId);
		assertNotNull(graphId2);
		assertEquals(sessionId, graphId2.sessionId());
		assertNotEquals(graphId1.toString(), graphId2.toString());

		// 测试空 sessionId 抛出异常
		assertThrows(IllegalArgumentException.class, () -> {
			graphProcess.createNewGraphId("");
		});

		assertThrows(IllegalArgumentException.class, () -> {
			graphProcess.createNewGraphId(null);
		});
	}

	@Test
	void testStopGraph() throws Exception {
		// 使用 CompletableFuture 来模拟一个永远不会完成的任务
		CompletableFuture<NodeOutput> neverCompleteFuture = new CompletableFuture<>();
		AsyncGenerator.Data<NodeOutput> hangingData = AsyncGenerator.Data.of(neverCompleteFuture);

		// 模拟 generator.next() 返回一个永远挂起的任务
		when(generator.next()).thenReturn(hangingData);

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		// 启动流程处理
		graphProcess.processStream(graphId, generator, sink);

		// 等待任务真正启动并进入挂起状态
		Thread.sleep(200);

		// 测试停止图执行 - 这个应该成功，因为任务正在运行
		boolean stopped = graphProcess.stopGraph(graphId);
		assertTrue(stopped, "应该能够停止正在运行的图任务");

		// 测试停止不存在的图
		GraphId nonExistentGraphId = new GraphId("non-existent", "non-existent-1");
		boolean notStopped = graphProcess.stopGraph(nonExistentGraphId);
		assertFalse(notStopped, "停止不存在的图应该返回 false");

		// 测试重复停止同一个图
		boolean alreadyStopped = graphProcess.stopGraph(graphId);
		assertFalse(alreadyStopped, "重复停止已停止的图应该返回 false");
	}

}
