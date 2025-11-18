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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Graph 执行取消示例
 * 演示如何取消图的执行
 */
public class CancellationExample {

	/**
	 * 示例 1: 使用 forEachAsync 消费流时取消
	 */
	public static void cancelWithForEachAsync(CompiledGraph compiledGraph, boolean mayInterruptIfRunning) {
		// 创建运行配置
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("test-thread")
				.build();

		// 准备输入数据
		Map<String, Object> inputData = new HashMap<>();
		// ... 添加输入数据

		// 执行图并获取流
		Flux<NodeOutput> stream = compiledGraph.stream(inputData, runnableConfig);

		// 从新线程在 500 毫秒后请求取消
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(500);
				// Flux 使用 dispose() 来取消
				System.out.println("请求取消执行");
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		// 异步处理每个输出
		var disposable = stream.subscribe(
				output -> System.out.println("当前迭代节点: " + output),
				error -> System.out.println("流错误: " + error.getMessage()),
				() -> System.out.println("流完成")
		);

		// 等待流完成或取消
		try {
			stream.blockLast();
		}
		catch (Exception e) {
			System.err.println("执行异常: " + e.getMessage());
		}

		// 验证是否已取消（Flux 使用 isDisposed 检查）
		System.out.println("是否已取消: " + disposable.isDisposed());
	}

	/**
	 * 示例 2: 使用迭代器消费流时取消
	 */
	public static void cancelWithIterator(CompiledGraph compiledGraph, boolean mayInterruptIfRunning) {
		// 创建运行配置
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("test-thread")
				.build();

		// 准备输入数据
		Map<String, Object> inputData = new HashMap<>();
		// ... 添加输入数据

		// 执行图并获取流
		Flux<NodeOutput> stream = compiledGraph.stream(inputData, runnableConfig);

		// 从新线程在 500 毫秒后请求取消
		var disposable = stream.subscribe(
				output -> {
					System.out.println("当前迭代节点: " + output);
				},
				error -> {
					System.out.println("流错误: " + error.getMessage());
				},
				() -> {
					System.out.println("流完成");
				}
		);

		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(500);
				disposable.dispose(); // 取消流
				System.out.println("已请求取消执行");
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		// 等待流完成或取消
		try {
			stream.blockLast();
		}
		catch (Exception e) {
			System.out.println("流被中断: " + e.getMessage());
		}

		// 验证取消状态
		System.out.println("是否已取消: " + disposable.isDisposed());
	}

	/**
	 * 检查取消状态
	 */
	public static void checkCancellationStatus(Disposable disposable) {
		if (disposable.isDisposed()) {
			System.out.println("流已被取消");
		}
		else {
			System.out.println("流仍在运行");
		}
	}

	public static void main(String[] args) {
		System.out.println("=== Graph 执行取消示例 ===\n");

		try {
			// 示例 1: 使用 forEachAsync 消费流时取消（需要 CompiledGraph）
			System.out.println("示例 1: 使用 forEachAsync 消费流时取消");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// cancelWithForEachAsync(compiledGraph, true);
			System.out.println();

			// 示例 2: 使用迭代器消费流时取消（需要 CompiledGraph）
			System.out.println("示例 2: 使用迭代器消费流时取消");
			System.out.println("注意: 此示例需要 CompiledGraph，跳过执行");
			// cancelWithIterator(compiledGraph, true);
			System.out.println();

			System.out.println("所有示例执行完成");
			System.out.println("提示: 请配置 CompiledGraph 后运行完整示例");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

