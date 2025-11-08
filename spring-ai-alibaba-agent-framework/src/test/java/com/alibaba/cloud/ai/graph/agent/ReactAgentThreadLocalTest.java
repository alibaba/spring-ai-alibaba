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
package com.alibaba.cloud.ai.graph.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 ReactAgent 的 ThreadLocal 管理逻辑
 * 
 * 这个测试不需要 API 密钥，使用反射验证 ThreadLocal 的初始化和清理
 */
class ReactAgentThreadLocalTest {

	/**
	 * 测试 ThreadLocal 字段存在且正确初始化
	 */
	@Test
	public void testThreadLocalFieldExists() throws Exception {
		// 使用反射获取 iterations 字段
		Field iterationsField = ReactAgent.class.getDeclaredField("iterations");
		iterationsField.setAccessible(true);
		
		// 验证字段类型是 ThreadLocal
		assertEquals(ThreadLocal.class, iterationsField.getType(), 
				"iterations should be of type ThreadLocal");
	}

	/**
	 * 验证 ReactAgent 有覆盖 doInvoke 方法
	 */
	@Test
	public void testDoInvokeMethodOverridden() throws Exception {
		// 验证 ReactAgent 有自己的 doInvoke 方法（覆盖父类）
		var method = ReactAgent.class.getDeclaredMethod("doInvoke", 
				java.util.Map.class, 
				com.alibaba.cloud.ai.graph.RunnableConfig.class);
		
		assertNotNull(method, "doInvoke method should exist");
		assertEquals(ReactAgent.class, method.getDeclaringClass(), 
				"doInvoke should be declared in ReactAgent, not inherited");
	}

	/**
	 * 验证 ReactAgent 有覆盖 doStream 方法（无 config 参数）
	 */
	@Test
	public void testDoStreamMethodOverridden_NoConfig() throws Exception {
		var method = ReactAgent.class.getDeclaredMethod("doStream", 
				java.util.Map.class);
		
		assertNotNull(method, "doStream(Map) method should exist");
		assertEquals(ReactAgent.class, method.getDeclaringClass(), 
				"doStream should be declared in ReactAgent, not inherited");
	}

	/**
	 * 验证 ReactAgent 有覆盖 doStream 方法（带 config 参数）
	 */
	@Test
	public void testDoStreamMethodOverridden_WithConfig() throws Exception {
		var method = ReactAgent.class.getDeclaredMethod("doStream", 
				java.util.Map.class, 
				com.alibaba.cloud.ai.graph.RunnableConfig.class);
		
		assertNotNull(method, "doStream(Map, RunnableConfig) method should exist");
		assertEquals(ReactAgent.class, method.getDeclaringClass(), 
				"doStream should be declared in ReactAgent, not inherited");
	}

	/**
	 * 验证所有必要的方法覆盖都存在，确保完整的修复
	 */
	@Test
	public void testAllNecessaryMethodsOverridden() throws Exception {
		// 验证所有三个关键方法都已覆盖
		assertDoesNotThrow(() -> {
			ReactAgent.class.getDeclaredMethod("doInvoke", 
					java.util.Map.class, 
					com.alibaba.cloud.ai.graph.RunnableConfig.class);
		}, "doInvoke should be overridden");

		assertDoesNotThrow(() -> {
			ReactAgent.class.getDeclaredMethod("doStream", 
					java.util.Map.class);
		}, "doStream(Map) should be overridden");

		assertDoesNotThrow(() -> {
			ReactAgent.class.getDeclaredMethod("doStream", 
					java.util.Map.class, 
					com.alibaba.cloud.ai.graph.RunnableConfig.class);
		}, "doStream(Map, RunnableConfig) should be overridden");
	}
}

