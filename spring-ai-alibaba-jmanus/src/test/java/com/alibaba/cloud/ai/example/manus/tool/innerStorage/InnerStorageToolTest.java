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
package com.alibaba.cloud.ai.example.manus.tool.innerStorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * InnerStorageTool 测试类
 */
public class InnerStorageToolTest {

	@TempDir
	Path tempDir;

	private InnerStorageTool innerStorageTool;

	private InnerStorageService innerStorageService;

	private final String testPlanId = "test-plan-001";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private InnerStorageTool.InnerStorageInput createInnerStorageInput(Map<String, Object> inputMap) throws Exception {
		String json = objectMapper.writeValueAsString(inputMap);
		return objectMapper.readValue(json, InnerStorageTool.InnerStorageInput.class);
	}

	@BeforeEach
	void setUp() {
		// 创建模拟的 ManusProperties，避免 ConfigService 依赖
		ManusProperties mockManusProperties = new ManusProperties() {
			private String baseDir = tempDir.toString();

			@Override
			public String getBaseDir() {
				return baseDir;
			}
		};

		// 创建 InnerStorageService
		innerStorageService = new InnerStorageService(mockManusProperties);

		// 使用测试专用构造函数创建 InnerStorageTool，直接指定工作目录
		innerStorageTool = new InnerStorageTool(innerStorageService, null, tempDir.toString());
		innerStorageTool.setPlanId(testPlanId);
	}

	@Test
	void testAppendToFile() throws Exception {
		// 追加内容到文件
		Map<String, Object> input = new HashMap<>();
		input.put("action", "append");
		input.put("file_name", "test.txt");
		input.put("content", "Hello, World!");

		InnerStorageTool.InnerStorageInput storageInput = createInnerStorageInput(input);
		ToolExecuteResult result = innerStorageTool.run(storageInput);

		assertTrue(result.getOutput().contains("文件创建成功并添加内容"));
	}

	@Test
	void testGetFileLines() throws Exception {
		// 添加多行内容
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "multiline.txt");
		appendInput.put("content", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5");

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		innerStorageTool.run(appendStorageInput);

		// 获取指定行
		Map<String, Object> getInput = new HashMap<>();
		getInput.put("action", "get_lines");
		getInput.put("file_name", "multiline.txt");
		getInput.put("start_line", 2);
		getInput.put("end_line", 4);

		InnerStorageTool.InnerStorageInput getStorageInput = createInnerStorageInput(getInput);
		ToolExecuteResult result = innerStorageTool.run(getStorageInput);

		assertTrue(result.getOutput().contains("第2-4行"));
		assertTrue(result.getOutput().contains("Line 2"));
		assertTrue(result.getOutput().contains("Line 3"));
	}

	@Test
	void testReplaceText() throws Exception {
		// 创建包含特定文本的文件
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "replace_test.txt");
		appendInput.put("content", "Hello World! This is a test.");

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		innerStorageTool.run(appendStorageInput);

		// 替换文本
		Map<String, Object> replaceInput = new HashMap<>();
		replaceInput.put("action", "replace");
		replaceInput.put("file_name", "replace_test.txt");
		replaceInput.put("source_text", "World");
		replaceInput.put("target_text", "Universe");

		InnerStorageTool.InnerStorageInput replaceStorageInput = createInnerStorageInput(replaceInput);
		ToolExecuteResult result = innerStorageTool.run(replaceStorageInput);

		assertTrue(result.getOutput().contains("文本替换成功"));

		// 验证替换结果
		Map<String, Object> getInput = new HashMap<>();
		getInput.put("action", "get_lines");
		getInput.put("file_name", "replace_test.txt");

		InnerStorageTool.InnerStorageInput getStorageInput = createInnerStorageInput(getInput);
		ToolExecuteResult getResult = innerStorageTool.run(getStorageInput);

		assertTrue(getResult.getOutput().contains("Hello Universe!"));
	}

	@Test
	void testGetCurrentState() {
		String state = innerStorageTool.getCurrentToolStateString();

		assertTrue(state.contains("InnerStorage 当前状态"));
		assertTrue(state.contains("Plan ID: " + testPlanId));
	}

	@Test
	void testErrorHandling() throws Exception {
		// 测试缺少必需参数的情况
		Map<String, Object> input = new HashMap<>();
		input.put("action", "append");
		// 缺少 file_name

		InnerStorageTool.InnerStorageInput storageInput = createInnerStorageInput(input);
		ToolExecuteResult result = innerStorageTool.run(storageInput);

		assertTrue(result.getOutput().contains("file_name参数是必需的"));
	}

	@Test
	void testSearchContent() throws Exception {
		// 创建包含搜索目标的文件
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "search_test.txt");
		appendInput.put("content", "这是一个包含Java关键词的测试文件。\n我们也有Python的内容。");

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		innerStorageTool.run(appendStorageInput);

		// 搜索关键词
		Map<String, Object> searchInput = new HashMap<>();
		searchInput.put("action", "search");
		searchInput.put("keyword", "Java");

		InnerStorageTool.InnerStorageInput searchStorageInput = createInnerStorageInput(searchInput);
		ToolExecuteResult result = innerStorageTool.run(searchStorageInput);

		assertTrue(result.getOutput().contains("Java"));
		assertTrue(result.getOutput().contains("找到"));
	}

	@Test
	void testListStoredContents() throws Exception {
		// 创建测试文件
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "list_test.txt");
		appendInput.put("content", "测试内容");

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		innerStorageTool.run(appendStorageInput);

		// 列出内容
		Map<String, Object> listInput = new HashMap<>();
		listInput.put("action", "list_contents");

		InnerStorageTool.InnerStorageInput listStorageInput = createInnerStorageInput(listInput);
		ToolExecuteResult result = innerStorageTool.run(listStorageInput);

		assertTrue(result.getOutput().contains("存储内容列表"));
		assertTrue(result.getOutput().contains("list_test.txt"));
	}

	@Test
	void testGetStoredContent() throws Exception {
		// 创建测试文件
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "content_test.txt");
		appendInput.put("content", "这是测试内容");

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		innerStorageTool.run(appendStorageInput);

		// 通过索引获取内容
		Map<String, Object> getInput = new HashMap<>();
		getInput.put("action", "get_content");
		getInput.put("content_id", "1");

		InnerStorageTool.InnerStorageInput getStorageInput = createInnerStorageInput(getInput);
		ToolExecuteResult result = innerStorageTool.run(getStorageInput);

		assertTrue(result.getOutput().contains("这是测试内容"));
	}

	@Test
	void testSmartContentProcessing() throws Exception {
		// 添加长内容
		Map<String, Object> appendInput = new HashMap<>();
		appendInput.put("action", "append");
		appendInput.put("file_name", "long_content.txt");
		appendInput.put("content", "这是一个很长的内容，用于测试智能内容处理功能。".repeat(10));

		InnerStorageTool.InnerStorageInput appendStorageInput = createInnerStorageInput(appendInput);
		ToolExecuteResult result = innerStorageTool.run(appendStorageInput);

		// 当内容过长时，应该返回摘要
		assertTrue(result.getOutput().contains("操作完成") || result.getOutput().contains("文件创建成功"));
	}

}
