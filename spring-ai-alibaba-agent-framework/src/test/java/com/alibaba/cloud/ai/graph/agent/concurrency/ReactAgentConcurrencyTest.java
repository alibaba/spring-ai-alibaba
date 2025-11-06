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
package com.alibaba.cloud.ai.graph.agent.concurrency;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发测试用例：验证 ReactAgent 在多线程环境下的线程安全性
 *
 * <p>测试场景：
 * <ul>
 *   <li>模拟 Spring Bean 单例模式：创建一个 ReactAgent 实例存储在类变量中</li>
 *   <li>多个线程并发调用同一个实例的不同方法（call、invoke、stream）</li>
 *   <li>验证 iterations 计数器的隔离性和正确性</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ReactAgentConcurrencyTest {

	private ChatModel chatModel;

	// 模拟 Spring Bean 单例：同一个 Agent 实例被多个请求共享
	private ReactAgent singletonAgent;

	@BeforeEach
	void setUp() throws Exception {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

		// 创建单例 Agent（模拟 Spring Bean 或 AgentStaticLoader 中的 Map 存储）
		this.singletonAgent = ReactAgent.builder()
				.name("singleton_agent")
				.model(chatModel)
				.maxIterations(5)  // 设置较小的上限便于测试
				.build();
	}

	/**
	 * 测试1：多线程并发调用 call() 方法
	 * 验证：每个线程的 iterations 计数器应该独立，从 0 开始
	 */
	@Test
	public void testConcurrentCallMethod() throws Exception {

		int threadCount = 10;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		
		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					// 等待所有线程就绪，然后同时开始
					startLatch.await();
					
					// 调用同一个 Agent 实例
					var result = singletonAgent.call("测试问题 " + threadId);
					
					// 验证返回结果不为空
					assertNotNull(result, "Thread " + threadId + " should get a result");
					successCount.incrementAndGet();
					
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		// 启动所有线程
		startLatch.countDown();
		
		// 等待所有线程完成（最多等待 60 秒）
		boolean finished = endLatch.await(60, TimeUnit.SECONDS);
		executor.shutdown();

		// 验证结果
		assertTrue(finished, "All threads should complete within timeout");
		assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
		assertEquals(threadCount, successCount.get(), "All threads should succeed");
	}

	/**
	 * 测试2：多线程并发调用 invoke() 方法
	 * 验证：invoke() 方法现在也应该正确重置 iterations
	 */
	@Test
	public void testConcurrentInvokeMethod() throws Exception {

		int threadCount = 10;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		
		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					
					// 调用 invoke() 方法
					Optional<OverAllState> result = singletonAgent.invoke("测试问题 " + threadId);
					
					// 验证返回结果
					assertTrue(result.isPresent(), "Thread " + threadId + " should get a result");
					successCount.incrementAndGet();
					
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		boolean finished = endLatch.await(60, TimeUnit.SECONDS);
		executor.shutdown();

		assertTrue(finished, "All threads should complete within timeout");
		assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
		assertEquals(threadCount, successCount.get(), "All threads should succeed");
	}

	/**
	 * 测试3：多线程并发调用 stream() 方法
	 * 验证：stream() 方法应该正确管理 ThreadLocal
	 */
	@Test
	public void testConcurrentStreamMethod() throws Exception {

		int threadCount = 10;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		
		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					
					// 调用 stream() 方法并收集所有结果
					List<NodeOutput> results = singletonAgent.stream("测试问题 " + threadId)
							.collectList()
							.block();
					
					// 验证返回结果
					assertNotNull(results, "Thread " + threadId + " should get results");
					assertFalse(results.isEmpty(), "Thread " + threadId + " should get non-empty results");
					successCount.incrementAndGet();
					
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		boolean finished = endLatch.await(60, TimeUnit.SECONDS);
		executor.shutdown();

		assertTrue(finished, "All threads should complete within timeout");
		assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
		assertEquals(threadCount, successCount.get(), "All threads should succeed");
	}

	/**
	 * 测试4：线程池复用场景
	 * 验证：同一个线程被复用处理多个请求时，iterations 应该正确重置
	 */
	@Test
	public void testThreadPoolReuse() throws Exception {

		// 使用只有 2 个线程的线程池，但提交 10 个任务
		// 这样每个线程会被复用多次
		int taskCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(taskCount);
		
		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < taskCount; i++) {
			final int taskId = i;
			executor.submit(() -> {
				try {
					// 混合使用不同的方法
					if (taskId % 3 == 0) {
						singletonAgent.call("任务 " + taskId);
					} else if (taskId % 3 == 1) {
						singletonAgent.invoke("任务 " + taskId);
					} else {
						singletonAgent.stream("任务 " + taskId).blockLast();
					}
					
					successCount.incrementAndGet();
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					latch.countDown();
				}
			});
		}

		boolean finished = latch.await(120, TimeUnit.SECONDS);
		executor.shutdown();

		assertTrue(finished, "All tasks should complete within timeout");
		assertTrue(exceptions.isEmpty(), "No exceptions should occur in thread pool reuse: " + exceptions);
		assertEquals(taskCount, successCount.get(), "All tasks should succeed");
	}

	/**
	 * 测试5：混合调用场景
	 * 验证：同时调用 call()、invoke()、stream() 三种方法时的线程安全性
	 */
	@Test
	public void testMixedMethodCalls() throws Exception {

		int threadsPerMethod = 5;
		int totalThreads = threadsPerMethod * 3;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(totalThreads);
		ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
		
		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger successCount = new AtomicInteger(0);

		// 启动 call() 线程
		for (int i = 0; i < threadsPerMethod; i++) {
			final int id = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					singletonAgent.call("call-" + id);
					successCount.incrementAndGet();
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		// 启动 invoke() 线程
		for (int i = 0; i < threadsPerMethod; i++) {
			final int id = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					singletonAgent.invoke("invoke-" + id);
					successCount.incrementAndGet();
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		// 启动 stream() 线程
		for (int i = 0; i < threadsPerMethod; i++) {
			final int id = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					singletonAgent.stream("stream-" + id).blockLast();
					successCount.incrementAndGet();
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		boolean finished = endLatch.await(120, TimeUnit.SECONDS);
		executor.shutdown();

		assertTrue(finished, "All threads should complete within timeout");
		assertTrue(exceptions.isEmpty(), "No exceptions should occur in mixed calls: " + exceptions);
		assertEquals(totalThreads, successCount.get(), "All threads should succeed");
	}
}

