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
package com.alibaba.cloud.ai.example.manus.tool.textOperator;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 AbstractSmartFileOperator 的自动存储功能
 */
class SmartFileOperatorTest {

	@TempDir
	Path tempDir;

	private InnerStorageService innerStorageService;

	private TestSmartFileOperator smartOperator;

	@BeforeEach
	void setUp() {
		ManusProperties properties = new ManusProperties();
		innerStorageService = new InnerStorageService(properties);
		smartOperator = new TestSmartFileOperator(innerStorageService, tempDir.toString());
		smartOperator.setPlanId("test-plan-001");
	}

	@Test
	void testShortContentNoStorage() {
		// 短内容不应该触发自动存储
		String shortContent = "这是一段短内容";
		ToolExecuteResult result = new ToolExecuteResult(shortContent);

		ToolExecuteResult processedResult = smartOperator.processResult(result, "test", "test.txt");

		assertEquals(shortContent, processedResult.getOutput());
		assertFalse(processedResult.getOutput().contains("完整内容已自动保存"));
	}

	@Test
	void testLongContentAutoStorage() {
		// 长内容应该触发自动存储
		StringBuilder longContent = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			longContent.append("这是第").append(i).append("行的测试内容，用于测试长内容的自动存储功能。\n");
		}

		ToolExecuteResult result = new ToolExecuteResult(longContent.toString());

		ToolExecuteResult processedResult = smartOperator.processResult(result, "test", "test.txt");

		assertTrue(processedResult.getOutput().contains("完整内容已自动保存"));
		assertTrue(processedResult.getOutput().contains("内容统计"));
		assertTrue(processedResult.getOutput().contains("存储文件:"));
		assertTrue(processedResult.getOutput().contains("内容ID:"));
	}

	@Test
	void testCustomThreshold() {
		// 测试自定义阈值
		String planId = "test-plan-001";
		String actualPlanId = smartOperator.getPlanId();
		System.out.println("Expected plan ID: " + planId);
		System.out.println("Actual plan ID: " + actualPlanId);

		// 确保我们为正确的 plan ID 设置阈值
		smartOperator.setContentThreshold(actualPlanId, 100); // 设置更小的阈值

		String mediumContent = "这是一段中等长度的内容，应该超过100字符的阈值。这是一段中等长度的内容，应该超过100字符的阈值。这是一段中等长度的内容，应该超过100字符的阈值。";
		System.out.println("Content length: " + mediumContent.length()); // 调试输出

		ToolExecuteResult result = new ToolExecuteResult(mediumContent);

		ToolExecuteResult processedResult = smartOperator.processResult(result, "test", "test.txt");
		System.out.println("Processed result: " + processedResult.getOutput()); // 调试输出

		// 如果阈值设置有效，这个长内容应该不等于原始内容（应该被摘要化）
		assertNotEquals(mediumContent, processedResult.getOutput());
		// 或者检查是否包含存储相关信息
		assertTrue(processedResult.getOutput().contains("完整内容已自动保存") || processedResult.getOutput().contains("内容统计")
				|| processedResult.getOutput().contains("存储文件"));
	}

	/**
	 * 测试用的 SmartFileOperator 实现
	 */
	private static class TestSmartFileOperator extends AbstractSmartFileOperator {

		private final InnerStorageService innerStorageService;

		private final String workingDirectory;

		private String planId;

		public TestSmartFileOperator(InnerStorageService innerStorageService, String workingDirectory) {
			this.innerStorageService = innerStorageService;
			this.workingDirectory = workingDirectory;
		}

		public void setPlanId(String planId) {
			this.planId = planId;
			innerStorageService.setPlanAgent(planId, "test-agent");
		}

		@Override
		protected String getWorkingDirectoryPath() {
			return workingDirectory;
		}

		@Override
		protected String getCurrentPlanId() {
			return planId;
		}

		@Override
		protected InnerStorageService getInnerStorageService() {
			return innerStorageService;
		}

		// 暴露 processResult 方法供测试使用
		public ToolExecuteResult processResult(ToolExecuteResult result, String operationType, String fileName) {
			return super.processResult(result, operationType, fileName);
		}

		// 获取当前Plan ID供测试使用
		public String getPlanId() {
			return planId;
		}

	}

}
