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
package com.alibaba.cloud.ai.example.manus.tool.mapreduce;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MapReduceTool 测试类
 */
public class MapReduceToolTest {

	@TempDir
	Path tempDir;

	private MapReduceTool mapReduceTool;

	private ManusProperties manusProperties;

	private UnifiedDirectoryManager unifiedDirectoryManager;

	private final String testPlanId = "test-plan-001";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MapReduceTool.MapReduceInput createMapReduceInput(String jsonInput) throws Exception {
		return objectMapper.readValue(jsonInput, MapReduceTool.MapReduceInput.class);
	}

	@BeforeEach
	void setUp() {
		// Create mock ManusProperties to avoid ConfigService dependency
		// Since CodeUtils.getWorkingDirectory() adds "extensions" directory under baseDir
		// We need to ensure the final working directory points to tempDir/extensions
		manusProperties = new ManusProperties() {
			private String baseDir = tempDir.toString();

			@Override
			public String getBaseDir() {
				return baseDir;
			}
		};

		// Create MapReduceSharedStateManager instance
		MapReduceSharedStateManager sharedStateManager = new MapReduceSharedStateManager();

		// Create UnifiedDirectoryManager
		unifiedDirectoryManager = new UnifiedDirectoryManager(manusProperties);

		// Create MapReduceTool instance using correct constructor with terminateColumns
		List<String> terminateColumns = Arrays.asList("title", "content"); // Default
																			// terminate
																			// columns for
																			// test
		mapReduceTool = new MapReduceTool(testPlanId, manusProperties, sharedStateManager, unifiedDirectoryManager,
				terminateColumns);
	}

	@Test
	void testSplitDataAction_SingleFile() throws Exception {
		// === 输入准备 ===
		// 使用测试资源文件作为数据源
		Path testResourcesFile = Path.of("src/test/resources/test_docs.md");
		assertTrue(Files.exists(testResourcesFile), "测试资源文件不存在");
		// 预期输入文件：src/test/resources/test_docs.md（包含测试文档内容）

		// 构建输入JSON - 工具接收的参数
		String input = """
				{
					"action": "split_data",
					"file_path": "%s",
					"return_columns": ["title", "content"]
				}
				""".formatted(testResourcesFile.toAbsolutePath().toString());
		// 工具输入：JSON格式，包含动作类型、文件路径和返回列配置

		// === 执行处理 ===
		MapReduceTool.MapReduceInput mapReduceInput = createMapReduceInput(input);
		ToolExecuteResult result = mapReduceTool.run(mapReduceInput);

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的字符串输出
		assertNotNull(result);
		assertTrue(result.getOutput().contains("切分文件成功"));
		assertTrue(result.getOutput().contains("创建了"));
		assertTrue(result.getOutput().contains("返回列：[title, content]"));
		// 预期输出：包含成功信息和处理详情的字符串

		// 验证分割文件确实被创建 - 内部状态
		List<String> splitResults = mapReduceTool.getSplitResults();
		assertFalse(splitResults.isEmpty());
		// 预期内部状态：splitResults 列表包含创建的任务目录路径

		// === 文件系统输出验证 ===
		// 验证输出目录结构
		// CodeUtils.getWorkingDirectory() 会返回 tempDir/extensions，所以 inner_storage 会在
		// extensions 目录下
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		assertTrue(Files.exists(tasksDir));
		assertTrue(Files.isDirectory(tasksDir));
		// 预期目录结构：tempDir/extensions/inner_storage/test-plan-001/tasks/

		// 验证任务目录存在及其内容
		for (String taskDir : splitResults) {
			assertTrue(Files.exists(Path.of(taskDir)));
			// 预期任务目录：tempDir/extensions/inner_storage/test-plan-001/tasks/task_001,
			// task_002, ...

			// 验证每个任务目录包含标准化文件
			Path taskPath = Path.of(taskDir);
			assertTrue(Files.exists(taskPath.resolve("input.md")));
			assertTrue(Files.exists(taskPath.resolve("status.json")));
			// 预期任务文件：
			// - input.md: 包含分割后的文档片段内容
			// - status.json: 包含任务状态信息（taskId, status, timestamp等）
			// 注意：此时 output.md 还未创建，因为只是数据分割阶段
		}
	}

	@Test
	void testSplitDataAction_Directory() throws Exception {
		// === 输入准备 ===
		// 创建测试目录和多个文件
		Path testDir = tempDir.resolve("test_data");
		Files.createDirectories(testDir);
		// 预期输入目录：tempDir/test_data/（包含多个文本文件，包括大文件）

		// 创建第一个测试文件
		Path testFile1 = testDir.resolve("data1.txt");
		StringBuilder testData1 = new StringBuilder();
		for (int i = 1; i <= 500; i++) { // 减少行数
			testData1.append(String.format("Line %d from file 1\n", i));
		}
		Files.write(testFile1, testData1.toString().getBytes());
		// 预期输入文件1：data1.txt（包含500行测试数据）

		// 创建第二个测试文件（较大文件，用于测试字符数自动切分）
		Path testFile2 = testDir.resolve("data2.txt");
		StringBuilder testData2 = new StringBuilder();
		// 创建一个较大的文件，确保字符数足够触发自动切分
		for (int i = 1; i <= 200; i++) { // 增加行数，确保总字符数更大
			testData2.append(String.format(
					"Line %d from file 2 - 这是较长的一行内容用于确保文件字符数足够大以触发自动分割功能。内容ID: %d, 时间戳: 2025-06-24T%02d:%02d:%02d\n",
					i, i, (i % 24), ((i * 13) % 60), ((i * 7) % 60)));
		}
		Files.write(testFile2, testData2.toString().getBytes());
		// 预期输入文件2：data2.txt（包含200行较长的测试数据，用于测试大文件基于字符数的自动切分）

		// 添加第三个更大的文件用于测试大文件的多重切分
		Path testFile3 = testDir.resolve("large_data.txt");
		StringBuilder testData3 = new StringBuilder();
		// 创建一个字符数很大的文件以确保会被切分成多个任务
		for (int i = 1; i <= 300; i++) { // 增加行数
			testData3.append(String.format(
					"Large file line %d - 这一行包含大量内容设计用于创建字符数很大的文件，该文件肯定会被分割成多个块。章节: %d, 子章节: %d, 详细信息: %s, 扩展内容带时间戳 %s 和随机数据 %d\n",
					i, (i / 100) + 1, (i / 10) % 10, "TEST_DATA_" + i,
					"2025-06-24T" + String.format("%02d:%02d:%02d", (i % 24), ((i * 13) % 60), ((i * 7) % 60)),
					i * 997));
		}
		Files.write(testFile3, testData3.toString().getBytes());
		// 预期输入文件3：large_data.txt（包含300行超长测试数据，确保基于字符数触发多重切分）

		// 构建输入JSON - 工具接收的参数
		String input = """
				{
					"action": "split_data",
					"file_path": "%s"
				}
				""".formatted(testDir.toString());
		// 工具输入：JSON格式，包含动作类型和目录路径

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的字符串输出
		assertNotNull(result);
		assertTrue(result.getOutput().contains("切分文件成功"));
		assertTrue(result.getOutput().contains("创建了"));
		// 预期输出：包含成功信息和处理详情的字符串

		// 验证分割文件数量 > 0 - 内部状态
		List<String> splitResults = mapReduceTool.getSplitResults();
		assertFalse(splitResults.isEmpty());
		// 应该有多个任务目录（三个原始文件的分割结果，特别是大文件应该基于字符数被分割成多个任务）
		assertTrue(splitResults.size() >= 3, "应该至少有3个任务目录（来自3个文件）");

		// 特别验证大文件的字符数自动切分效果
		// 由于 large_data.txt 字符数很大，应该被分割成多个任务
		assertTrue(splitResults.size() >= 5, "大文件应该基于字符数触发自动切分，生成更多任务目录");

		// 输出详细信息用于调试
		System.out.println("=== 文件切分结果统计 ===");
		System.out.println("总任务数: " + splitResults.size());

		// 统计各文件的大小和预期切分情况
		long file1Size = Files.size(testFile1);
		long file2Size = Files.size(testFile2);
		long file3Size = Files.size(testFile3);
		System.out.println("文件1大小: " + file1Size + " bytes");
		System.out.println("文件2大小: " + file2Size + " bytes");
		System.out.println("文件3大小: " + file3Size + " bytes");

		// 验证大文件确实比小文件大
		assertTrue(file2Size > file1Size, "第二个文件应该比第一个文件大");
		assertTrue(file3Size > file2Size, "第三个文件应该是最大的文件");
		assertTrue(file3Size > 50000, "大文件应该超过50KB以确保触发字符数切分");
		// 预期内部状态：splitResults 列表包含>=5个任务目录路径（大文件的字符数自动切分效果）
		// 预期文件系统输出：
		// -
		// tempDir/extensions/inner_storage/test-plan-001/tasks/task_001/（来自data1.txt的分割）
		// -
		// tempDir/extensions/inner_storage/test-plan-001/tasks/task_002/（来自data2.txt的分割）
		// -
		// tempDir/extensions/inner_storage/test-plan-001/tasks/task_003/（来自large_data.txt的第一个分割）
		// -
		// tempDir/extensions/inner_storage/test-plan-001/tasks/task_004/（来自large_data.txt的第二个分割）
		// - ... 更多任务目录（大文件的多重字符数自动切分结果）
		// 每个任务目录包含：input.md, status.json

		// === 文件系统输出验证 ===
		// 验证输出目录结构
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		assertTrue(Files.exists(tasksDir));
		assertTrue(Files.isDirectory(tasksDir));

		// 验证每个任务目录的结构和内容
		for (String taskDir : splitResults) {
			assertTrue(Files.exists(Path.of(taskDir)), "任务目录应该存在: " + taskDir);

			Path taskPath = Path.of(taskDir);
			assertTrue(Files.exists(taskPath.resolve("input.md")), "input.md 应该存在");
			assertTrue(Files.exists(taskPath.resolve("status.json")), "status.json 应该存在");

			// 验证输入文件不为空
			assertTrue(Files.size(taskPath.resolve("input.md")) > 0, "input.md 不应该为空");

			// 验证状态文件格式
			String statusContent = Files.readString(taskPath.resolve("status.json"));
			assertTrue(statusContent.contains("\"taskId\""), "状态文件应该包含taskId");
			assertTrue(statusContent.contains("\"status\""), "状态文件应该包含status");
		}

		// 验证大文件切分的效果：检查是否有多个任务来自同一个大文件
		int tasksFromLargeFile = 0;
		for (String taskDir : splitResults) {
			Path inputFile = Path.of(taskDir).resolve("input.md");
			String inputContent = Files.readString(inputFile);
			if (inputContent.contains("Large file line")) {
				tasksFromLargeFile++;
			}
		}
		assertTrue(tasksFromLargeFile >= 2, "大文件应该基于字符数被分割成至少2个任务，实际: " + tasksFromLargeFile);
		System.out.println("大文件被分割成的任务数: " + tasksFromLargeFile);
		System.out.println("字符数分割限制: " + 1000 + " 字符/任务");
	}

	@Test
	void testRecordMapOutput() throws Exception {
		// === 输入准备 ===
		// 首先创建一个任务目录（模拟先运行split_data）
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		Path taskDir = tasksDir.resolve("task_001");
		Files.createDirectories(taskDir);
		// 预期输入目录：tempDir/extensions/inner_storage/test-plan-001/tasks/task_001/

		// 创建 input.md 文件（模拟split_data阶段创建的输入文件）
		Path inputFile = taskDir.resolve("input.md");
		Files.write(inputFile, "# 测试文档片段\n\n测试内容".getBytes());
		// 预期输入文件：input.md（包含待处理的文档片段）

		// 创建初始状态文件（模拟split_data阶段创建的状态文件）
		Path statusFile = taskDir.resolve("status.json");
		String initialStatus = """
				{
					"taskId": "task_001",
					"inputFile": "%s",
					"status": "pending",
					"timestamp": "2025-01-01T12:00:00"
				}
				""".formatted(inputFile.toAbsolutePath().toString());
		Files.write(statusFile, initialStatus.getBytes());
		// 预期输入状态文件：status.json（包含pending状态的任务信息）

		// 测试数据 - 模拟Map任务处理后的结果
		String testContent = "这是Map任务处理后的结果内容，包含一些处理后的数据，任务ID: task_001";
		String taskId = "task_001";
		String status = "completed";
		// 预期处理内容：Map任务的输出结果

		// 构建输入JSON - 工具接收的参数
		String input = """
				{
					"action": "record_map_output",
					"content": "%s",
					"task_id": "%s",
					"status": "%s"
				}
				""".formatted(testContent, taskId, status);
		// 工具输入：JSON格式，包含动作类型、处理结果内容、任务ID和状态

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的字符串输出
		assertNotNull(result);
		assertTrue(result.getOutput().contains("任务 task_001 状态已记录"));
		assertTrue(result.getOutput().contains("completed"));
		assertTrue(result.getOutput().contains("输出文件：output.md"));
		// 预期输出：包含任务状态记录成功信息的字符串

		// === 文件系统输出验证 ===
		// 验证任务目录结构
		assertTrue(Files.exists(taskDir));
		assertTrue(Files.exists(taskDir.resolve("input.md")));
		assertTrue(Files.exists(taskDir.resolve("status.json")));
		assertTrue(Files.exists(taskDir.resolve("output.md")));
		// 预期目录结构：
		// - input.md: 原始输入文件（保持不变）
		// - status.json: 更新后的状态文件（状态从pending变为completed）
		// - output.md: 新创建的输出文件（包含处理结果）

		// 验证输出文件内容
		String outputContent = Files.readString(taskDir.resolve("output.md"));
		assertTrue(outputContent.contains("# 任务处理结果"));
		assertTrue(outputContent.contains("**任务ID:** task_001"));
		assertTrue(outputContent.contains("**处理状态:** completed"));
		assertTrue(outputContent.contains(testContent));
		// 预期输出文件内容：格式化的Markdown文档，包含任务信息和处理结果

		// 验证状态文件内容
		String statusContent = Files.readString(statusFile);
		assertTrue(statusContent.contains("\"taskId\":\"task_001\""));
		assertTrue(statusContent.contains("\"status\":\"completed\""));
		// 预期状态文件内容：更新后的JSON，状态变为completed，包含时间戳等信息
	}

	@Test
	void testRecordMapOutput_Failed() throws Exception {
		// === 输入准备 ===
		// 首先创建一个任务目录（模拟先运行split_data）
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		Path taskDir = tasksDir.resolve("task_002");
		Files.createDirectories(taskDir);
		// 预期输入目录：tempDir/extensions/inner_storage/test-plan-001/tasks/task_002/

		// 创建 input.md 文件（模拟split_data阶段创建的输入文件）
		Path inputFile = taskDir.resolve("input.md");
		Files.write(inputFile, "# 测试文档片段\n\n测试内容".getBytes());
		// 预期输入文件：input.md（包含待处理的文档片段）

		// 创建初始状态文件（模拟split_data阶段创建的状态文件）
		Path statusFile = taskDir.resolve("status.json");
		String initialStatus = """
				{
					"taskId": "task_002",
					"inputFile": "%s",
					"status": "pending",
					"timestamp": "2025-01-01T12:00:00"
				}
				""".formatted(inputFile.toAbsolutePath().toString());
		Files.write(statusFile, initialStatus.getBytes());
		// 预期输入状态文件：status.json（包含pending状态的任务信息）

		// 测试失败状态的记录 - 模拟Map任务处理失败的场景
		String testContent = "处理失败的内容";
		String taskId = "task_002";
		String status = "failed";
		// 预期处理内容：Map任务失败的结果

		// 构建输入JSON - 工具接收的参数
		String input = """
				{
					"action": "record_map_output",
					"content": "%s",
					"task_id": "%s",
					"status": "%s"
				}
				""".formatted(testContent, taskId, status);
		// 工具输入：JSON格式，包含动作类型、失败内容、任务ID和失败状态

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的字符串输出
		assertNotNull(result);
		assertTrue(result.getOutput().contains("任务 task_002 状态已记录"));
		assertTrue(result.getOutput().contains("failed"));
		// 预期输出：包含任务失败状态记录成功信息的字符串

		// === 文件系统输出验证 ===
		// 验证状态文件内容
		assertTrue(Files.exists(statusFile));
		String statusContent = Files.readString(statusFile);
		assertTrue(statusContent.contains("\"status\":\"failed\""));
		// 预期状态文件内容：更新后的JSON，状态变为failed
		// 预期目录结构：
		// - input.md: 原始输入文件（保持不变）
		// - status.json: 更新后的状态文件（状态从pending变为failed）
		// - output.md: 可能创建的输出文件（包含失败信息）
	}

	@Test
	void testInvalidAction() throws Exception {
		// === 输入准备 ===
		// 测试无效的 action - 模拟用户传入不支持的动作类型
		String input = """
				{
					"action": "invalid_action",
					"file_path": "/some/path"
				}
				""";
		// 工具输入：JSON格式，包含无效的动作类型和文件路径

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的错误信息字符串
		assertNotNull(result);
		assertTrue(result.getOutput().contains("未知操作: invalid_action"));
		assertTrue(result.getOutput().contains("支持的操作: split_data, record_map_output"));
		// 预期输出：包含错误信息和支持操作列表的字符串
		// 预期文件系统输出：无文件系统变更（因为操作无效）
	}

	@Test
	void testMissingRequiredParameters() throws Exception {
		// === 输入准备 - 测试缺少必需参数的场景 ===

		// 测试split_data缺少file_path参数
		String input1 = """
				{
					"action": "split_data"
				}
				""";
		// 工具输入1：JSON格式，只包含动作类型，缺少必需的file_path参数

		// === 执行处理1 ===
		ToolExecuteResult result1 = mapReduceTool.run(createMapReduceInput(input1));

		// === 输出验证1 ===
		assertNotNull(result1);
		assertTrue(result1.getOutput().contains("错误：file_path参数是必需的"));
		// 预期输出1：包含缺少file_path参数错误信息的字符串

		// 测试record_map_output缺少task_id参数
		String input2 = """
				{
					"action": "record_map_output",
					"content": "test content"
				}
				""";
		// 工具输入2：JSON格式，包含动作类型和内容，但缺少必需的task_id参数

		// === 执行处理2 ===
		ToolExecuteResult result2 = mapReduceTool.run(createMapReduceInput(input2));

		// === 输出验证2 ===
		assertNotNull(result2);
		assertTrue(result2.getOutput().contains("错误：task_id参数是必需的"));
		// 预期输出2：包含缺少task_id参数错误信息的字符串
		// 预期文件系统输出：无文件系统变更（因为参数验证失败）
	}

	@Test
	void testFileNotExists() throws Exception {
		// === 输入准备 ===
		// 测试不存在的文件 - 模拟用户传入不存在的文件路径
		String input = """
				{
					"action": "split_data",
					"file_path": "/non/existent/file.txt"
				}
				""";
		// 工具输入：JSON格式，包含动作类型和不存在的文件路径

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		// 验证工具执行结果 - 返回的错误信息字符串
		assertNotNull(result);
		assertTrue(result.getOutput().contains("错误：文件或目录不存在"));
		// 预期输出：包含文件不存在错误信息的字符串
		// 预期文件系统输出：无文件系统变更（因为源文件不存在）
	}

	@Test
	void testGetCurrentToolStateString() {
		// === 输入准备 ===
		// 无需特殊输入准备 - 测试工具状态查询功能
		// 预期输入状态：工具初始状态（无任务目录）

		// === 执行处理 ===
		String stateString = mapReduceTool.getCurrentToolStateString();

		// === 输出验证 ===
		// 验证工具状态字符串输出
		assertNotNull(stateString);
		assertTrue(stateString.contains("MapReduceTool 当前状态"));
		assertTrue(stateString.contains("Plan ID: " + testPlanId));
		assertTrue(stateString.contains("任务目录数: 0"));
		// 预期输出：包含工具状态信息的格式化字符串
		// 包含：工具名称、计划ID、任务目录数量等信息
		// 预期文件系统输出：无文件系统变更（只是状态查询）
	}

	@Test
	void testCleanup() {
		// === 输入准备 ===
		// 先添加一些状态
		mapReduceTool.getSplitResults().clear(); // 确保初始状态干净
		// 预期输入状态：工具内部状态（splitResults列表）

		// === 执行处理 ===
		// 执行清理操作
		mapReduceTool.cleanup(testPlanId);

		// === 输出验证 ===
		// 验证状态被清理 - 内部状态验证
		assertTrue(mapReduceTool.getSplitResults().isEmpty());
		String stateString = mapReduceTool.getCurrentToolStateString();
		assertTrue(stateString.contains("任务目录数: 0"));
		// 预期输出：内部状态被清理，splitResults列表为空
		// 预期状态字符串：显示任务目录数为0
		// 预期文件系统输出：可能清理计划相关的目录和文件
		// （具体清理行为取决于cleanup方法的实现）
	}

	@Test
	void testGetToolMetadata() {
		// === 输入准备 ===
		// 无需特殊输入准备 - 测试工具元数据获取功能
		// 预期输入状态：工具实例的元数据信息

		// === 执行处理与输出验证 ===
		// 测试工具名称
		assertEquals("map_reduce_tool", mapReduceTool.getName());
		// 预期输出：工具名称字符串 "map_reduce_tool"

		// 测试工具描述
		assertNotNull(mapReduceTool.getDescription());
		assertTrue(mapReduceTool.getDescription().contains("数据分割工具"));
		// 预期输出：包含"数据分割工具"的描述字符串

		// 测试工具参数信息
		assertNotNull(mapReduceTool.getParameters());
		// 预期输出：非空的参数信息对象

		// 测试输入类型
		assertEquals(MapReduceTool.MapReduceInput.class, mapReduceTool.getInputType());
		// 预期输出：String.class（工具接受字符串类型输入）

		// 测试返回直接性
		assertFalse(mapReduceTool.isReturnDirect());
		// 预期输出：false（工具不直接返回结果）

		// 测试服务组
		assertEquals("data-processing", mapReduceTool.getServiceGroup());
		// 预期输出：服务组名称字符串 "data-processing"

		// 预期文件系统输出：无文件系统变更（只是元数据查询）
	}

	@Test
	void testMultipleMapOutputs() throws Exception {
		// === 输入准备 ===
		// 首先创建任务目录结构
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		Files.createDirectories(tasksDir);
		// 预期输入目录：tempDir/extensions/inner_storage/test-plan-001/tasks/

		// 测试记录多个Map任务输出
		String[] taskIds = { "task_001", "task_002", "task_003" };
		String[] contents = { "第一个任务的输出内容", "第二个任务的输出内容", "第三个任务的输出内容" };
		// 预期输入数据：3个任务ID和对应的处理结果内容

		// 为每个任务创建目录和初始文件（模拟split_data阶段创建的文件）
		for (String taskId : taskIds) {
			Path taskDir = tasksDir.resolve(taskId);
			Files.createDirectories(taskDir);

			// 创建 input.md
			Files.write(taskDir.resolve("input.md"), "# 测试文档片段\n\n测试内容".getBytes());

			// 创建初始状态文件
			String initialStatus = """
					{
						"taskId": "%s",
						"inputFile": "%s",
						"status": "pending",
						"timestamp": "2025-01-01T12:00:00"
					}
					""".formatted(taskId, taskDir.resolve("input.md").toAbsolutePath().toString());
			Files.write(taskDir.resolve("status.json"), initialStatus.getBytes());
		}
		// 预期输入文件系统：3个任务目录，每个包含input.md和status.json（pending状态）

		// === 执行处理 ===
		// 记录多个任务的输出
		for (int i = 0; i < taskIds.length; i++) {
			String input = """
					{
						"action": "record_map_output",
						"content": "%s",
						"task_id": "%s",
						"status": "completed"
					}
					""".formatted(contents[i], taskIds[i]);
			// 工具输入：JSON格式，依次记录每个任务的输出

			ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));
			assertNotNull(result);
			assertTrue(result.getOutput().contains("状态已记录"));
			// 预期输出：每次调用都返回状态记录成功的信息
		}

		// === 输出验证 ===
		// 验证每个任务的输出文件都存在
		for (String taskId : taskIds) {
			Path taskDir = tasksDir.resolve(taskId);
			assertTrue(Files.exists(taskDir.resolve("output.md")));
			assertTrue(Files.exists(taskDir.resolve("status.json")));

			// 验证状态文件内容
			String statusContent = Files.readString(taskDir.resolve("status.json"));
			assertTrue(statusContent.contains("\"taskId\":\"" + taskId + "\""));
			assertTrue(statusContent.contains("\"status\":\"completed\""));
		}
		// 预期文件系统输出：3个任务目录，每个包含：
		// - input.md: 原始输入文件（保持不变）
		// - status.json: 更新后的状态文件（状态变为completed）
		// - output.md: 新创建的输出文件（包含各自的处理结果）
	}

	@Test
	void testRelativePathHandling() throws Exception {
		// === 输入准备 ===
		// 测试相对路径处理
		Path testFile = tempDir.resolve("relative_test.txt");
		Files.write(testFile, "test content for relative path".getBytes());
		// 预期输入文件：tempDir/relative_test.txt（包含测试内容）

		// 使用相对路径（只有文件名）
		String relativePath = testFile.getFileName().toString();
		String input = """
				{
					"action": "split_data",
					"file_path": "%s"
				}
				""".formatted(relativePath);
		// 工具输入：JSON格式，包含动作类型和相对路径（仅文件名）

		// === 执行处理 ===
		// 由于相对路径解析可能找不到文件（基于工作目录），这个测试可能失败
		// 但我们测试工具能正确处理相对路径的逻辑
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		assertNotNull(result);
		// 结果可能是错误信息，但不应该抛出异常
		// 预期输出：可能是成功信息（如果相对路径解析成功）或错误信息（如果文件未找到）
		// 预期文件系统输出：
		// - 如果路径解析成功：创建任务目录和相关文件
		// - 如果路径解析失败：无文件系统变更
		// 注意：此测试主要验证相对路径处理的健壮性，不应导致程序崩溃
	}

	@Test
	void testTaskStatusManagement() throws Exception {
		// === 输入准备 ===
		// 测试任务状态管理功能
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		Path taskDir = tasksDir.resolve("task_status_test");
		Files.createDirectories(taskDir);
		// 预期输入目录：tempDir/extensions/inner_storage/test-plan-001/tasks/task_status_test/

		// 创建 input.md 文件
		Path inputFile = taskDir.resolve("input.md");
		Files.write(inputFile, "# 状态管理测试文档\n\n测试任务状态变更流程".getBytes());
		// 预期输入文件：input.md（包含测试文档内容）

		// 创建初始状态文件
		Path statusFile = taskDir.resolve("status.json");
		String initialStatus = """
				{
					"taskId": "task_status_test",
					"inputFile": "%s",
					"status": "pending",
					"timestamp": "2025-01-01T12:00:00"
				}
				""".formatted(inputFile.toAbsolutePath().toString());
		Files.write(statusFile, initialStatus.getBytes());
		// 预期输入状态文件：status.json（包含pending状态）

		// === 执行处理 - 测试状态从pending到completed的转换 ===
		String input = """
				{
					"action": "record_map_output",
					"content": "任务状态管理测试完成，状态已从pending变更为completed",
					"task_id": "task_status_test",
					"status": "completed"
				}
				""";
		// 工具输入：JSON格式，记录任务完成状态

		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		assertNotNull(result);
		assertTrue(result.getOutput().contains("task_status_test"));
		assertTrue(result.getOutput().contains("completed"));
		// 预期输出：包含任务ID和完成状态的确认信息

		// 验证状态文件已更新
		assertTrue(Files.exists(statusFile));
		String updatedStatusContent = Files.readString(statusFile);
		assertTrue(updatedStatusContent.contains("\"status\":\"completed\""));
		assertTrue(updatedStatusContent.contains("\"taskId\":\"task_status_test\""));
		// 预期状态文件：更新为completed状态

		// 验证输出文件已创建
		assertTrue(Files.exists(taskDir.resolve("output.md")));
		String outputContent = Files.readString(taskDir.resolve("output.md"));
		assertTrue(outputContent.contains("# 任务处理结果"));
		assertTrue(outputContent.contains("task_status_test"));
		assertTrue(outputContent.contains("completed"));
		// 预期输出文件：包含格式化的任务结果信息
	}

	@Test
	void testLargeFileAutoSplitting() throws Exception {
		// === 输入准备 ===
		// 测试大文件自动分割功能，验证DEFAULT_SPLIT_SIZE的工作机制（按字符数分割）
		Path testFile = tempDir.resolve("large_auto_split.txt");
		StringBuilder largeContent = new StringBuilder();

		// 创建超过DEFAULT_SPLIT_SIZE（1000字符）的文件
		// 每行大约50个字符，需要约25行才能超过1000字符
		int linesPerChunk = 25;
		int totalLines = linesPerChunk * 3; // 应该分割成3个任务
		for (int i = 1; i <= totalLines; i++) {
			largeContent.append(String.format("Line %02d: 测试内容用于验证字符数分割功能\n", i)); // 每行约30字符
		}
		Files.write(testFile, largeContent.toString().getBytes());
		// 预期输入文件：large_auto_split.txt（包含足够字符数，应触发字符数分割）

		// === 执行处理 ===
		String input = """
				{
					"action": "split_data",
					"file_path": "%s",
					"return_columns": ["line_number", "content"]
				}
				""".formatted(testFile.toAbsolutePath().toString());
		// 工具输入：JSON格式，包含分割动作和返回列配置

		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));

		// === 输出验证 ===
		assertNotNull(result);
		assertTrue(result.getOutput().contains("切分文件成功"));
		assertTrue(result.getOutput().contains("返回列：[line_number, content]"));
		// 预期输出：包含成功信息和返回列配置

		// 验证分割结果数量（基于字符数分割）
		List<String> splitResults = mapReduceTool.getSplitResults();
		assertTrue(splitResults.size() >= 2, "文件应该基于字符数被分割成多个任务");
		// 预期分割结果：多个任务目录（基于字符数限制）

		// 验证每个任务目录的字符数都接近或不超过DEFAULT_SPLIT_SIZE
		for (int i = 0; i < splitResults.size(); i++) {
			String taskDir = splitResults.get(i);
			assertTrue(Files.exists(Path.of(taskDir)));

			// 验证任务文件存在
			Path taskPath = Path.of(taskDir);
			assertTrue(Files.exists(taskPath.resolve("input.md")));
			assertTrue(Files.exists(taskPath.resolve("status.json")));

			// 读取任务内容并检查字符数（除了最后一个任务）
			String inputContent = Files.readString(taskPath.resolve("input.md"));
			assertTrue(inputContent.contains("# 文档片段"));
			assertTrue(inputContent.contains("large_auto_split.txt"));

			// 提取实际的文档内容（去除Markdown格式）
			String[] lines = inputContent.split("\n");
			StringBuilder actualContent = new StringBuilder();
			boolean inCodeBlock = false;
			for (String line : lines) {
				if (line.equals("```")) {
					inCodeBlock = !inCodeBlock;
					continue;
				}
				if (inCodeBlock) {
					actualContent.append(line).append("\n");
				}
			}

			// 验证字符数限制（最后一个任务可能少于DEFAULT_SPLIT_SIZE）
			int contentLength = actualContent.toString().length();
			if (i < splitResults.size() - 1) {
				// 非最后一个任务应该接近DEFAULT_SPLIT_SIZE
				assertTrue(contentLength <= 1200, // 允许一些Markdown格式的额外字符
						"任务 " + (i + 1) + " 的内容字符数 (" + contentLength + ") 应该接近DEFAULT_SPLIT_SIZE");
			}
		}

		// 输出统计信息用于验证
		System.out.println("=== 大文件字符数分割测试结果 ===");
		System.out.println("原始文件字符数: " + largeContent.length());
		System.out.println("分割任务数: " + splitResults.size());
		System.out.println("每任务字符数限制: " + 1000 + " (DEFAULT_SPLIT_SIZE)");
	}

	@Test
	void testMapOutputWithDifferentStatuses() throws Exception {
		// === 输入准备 ===
		// 测试不同状态的Map输出记录（completed和failed）
		Path planDir = tempDir.resolve("extensions").resolve("inner_storage").resolve(testPlanId);
		Path tasksDir = planDir.resolve("tasks");
		Files.createDirectories(tasksDir);

		// 创建两个测试任务
		String[] taskIds = { "task_completed", "task_failed" };
		String[] statuses = { "completed", "failed" };
		String[] contents = { "任务成功完成，处理了所有数据并生成了预期结果", "任务执行失败，遇到数据格式错误：无法解析第42行的JSON数据" };

		// 为每个任务创建初始结构
		for (String taskId : taskIds) {
			Path taskDir = tasksDir.resolve(taskId);
			Files.createDirectories(taskDir);

			// 创建 input.md
			Files.write(taskDir.resolve("input.md"), "# 状态测试文档\n\n测试不同状态处理".getBytes());

			// 创建初始状态文件
			String initialStatus = """
					{
						"taskId": "%s",
						"inputFile": "%s",
						"status": "pending",
						"timestamp": "2025-01-01T12:00:00"
					}
					""".formatted(taskId, taskDir.resolve("input.md").toAbsolutePath().toString());
			Files.write(taskDir.resolve("status.json"), initialStatus.getBytes());
		}
		// 预期输入文件系统：2个任务目录，每个包含input.md和status.json（pending状态）

		// === 执行处理 ===
		// 依次处理每个任务，使用不同的状态
		for (int i = 0; i < taskIds.length; i++) {
			String input = """
					{
						"action": "record_map_output",
						"content": "%s",
						"task_id": "%s",
						"status": "%s"
					}
					""".formatted(contents[i], taskIds[i], statuses[i]);
			// 工具输入：JSON格式，记录不同状态的任务输出

			ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));
			assertNotNull(result);
			assertTrue(result.getOutput().contains(taskIds[i]));
			assertTrue(result.getOutput().contains(statuses[i]));
			// 预期输出：包含任务ID和对应状态的确认信息
		}

		// === 输出验证 ===
		// 验证completed任务
		Path completedTaskDir = tasksDir.resolve("task_completed");
		assertTrue(Files.exists(completedTaskDir.resolve("output.md")));
		String completedOutput = Files.readString(completedTaskDir.resolve("output.md"));
		assertTrue(completedOutput.contains("completed"));
		assertTrue(completedOutput.contains("任务成功完成"));

		String completedStatus = Files.readString(completedTaskDir.resolve("status.json"));
		assertTrue(completedStatus.contains("\"status\":\"completed\""));
		// 预期completed任务：输出文件包含成功信息，状态文件标记为completed

		// 验证failed任务
		Path failedTaskDir = tasksDir.resolve("task_failed");
		assertTrue(Files.exists(failedTaskDir.resolve("output.md")));
		String failedOutput = Files.readString(failedTaskDir.resolve("output.md"));
		assertTrue(failedOutput.contains("failed"));
		assertTrue(failedOutput.contains("数据格式错误"));

		String failedStatus = Files.readString(failedTaskDir.resolve("status.json"));
		assertTrue(failedStatus.contains("\"status\":\"failed\""));
		// 预期failed任务：输出文件包含错误信息，状态文件标记为failed
	}

	@Test
	void testToolStateStringAccuracy() throws Exception {
		// === 输入准备 ===
		// 测试getCurrentToolStateString方法的准确性
		// 初始状态验证
		String initialState = mapReduceTool.getCurrentToolStateString();
		assertTrue(initialState.contains("Plan ID: " + testPlanId));
		assertTrue(initialState.contains("任务目录数: 0"));
		assertTrue(initialState.contains("最后处理文件: 无"));
		// 预期初始状态：包含planId，任务目录数为0，无处理文件

		// === 执行处理 ===
		// 执行一次文件分割操作
		Path testFile = tempDir.resolve("state_test.txt");
		StringBuilder testContent = new StringBuilder();
		// 创建一个小文件，确保只生成1个任务（少于1000字符）
		for (int i = 1; i <= 20; i++) { // 减少行数，确保总字符数小于1000
			testContent.append("State test line ").append(i).append("\n");
		}
		Files.write(testFile, testContent.toString().getBytes());

		String input = """
				{
					"action": "split_data",
					"file_path": "%s"
				}
				""".formatted(testFile.toAbsolutePath().toString());

		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));
		assertNotNull(result);
		assertTrue(result.getOutput().contains("切分文件成功"));

		// === 输出验证 ===
		// 验证状态字符串更新
		String updatedState = mapReduceTool.getCurrentToolStateString();
		assertTrue(updatedState.contains("Plan ID: " + testPlanId));
		assertTrue(updatedState.contains("任务目录数: 1")); // 20行小文件应该只创建1个任务
		assertTrue(updatedState.contains("最后操作结果: 已完成"));
		// 预期更新状态：任务目录数增加，操作结果显示已完成

		// 验证cleanup后的状态
		mapReduceTool.cleanup(testPlanId);
		String cleanedState = mapReduceTool.getCurrentToolStateString();
		assertTrue(cleanedState.contains("任务目录数: 0"));
		assertTrue(cleanedState.contains("最后处理文件: 无"));
		assertTrue(cleanedState.contains("最后操作结果: 无"));
		// 预期清理后状态：所有计数器和状态重置为初始值
	}

	@Test
	void testTaskDirectoryStructureConsistency() throws Exception {
		// === 输入准备 ===
		// 测试任务目录结构的一致性，确保与InnerStorageService兼容
		Path testFile = tempDir.resolve("structure_test.txt");
		Files.write(testFile, "Directory structure consistency test content".getBytes());

		String input = """
				{
					"action": "split_data",
					"file_path": "%s"
				}
				""".formatted(testFile.toAbsolutePath().toString());

		// === 执行处理 ===
		ToolExecuteResult result = mapReduceTool.run(createMapReduceInput(input));
		assertNotNull(result);
		assertTrue(result.getOutput().contains("切分文件成功"));

		// === 输出验证 ===
		// 验证目录结构符合预期模式：extensions/inner_storage/{planId}/tasks/{taskId}
		List<String> splitResults = mapReduceTool.getSplitResults();
		assertFalse(splitResults.isEmpty());

		String taskDir = splitResults.get(0);
		Path taskPath = Path.of(taskDir);

		// 验证路径结构
		assertTrue(taskPath.toString().contains("extensions"));
		assertTrue(taskPath.toString().contains("inner_storage"));
		assertTrue(taskPath.toString().contains(testPlanId));
		assertTrue(taskPath.toString().contains("tasks"));
		assertTrue(taskPath.toString().contains("task_001"));
		// 预期路径结构：符合InnerStorageService的目录约定

		// 验证必需文件存在
		assertTrue(Files.exists(taskPath.resolve("input.md")));
		assertTrue(Files.exists(taskPath.resolve("status.json")));
		assertFalse(Files.exists(taskPath.resolve("output.md"))); // 初始时不应存在
		// 预期文件结构：包含input.md和status.json，output.md在Map阶段后创建

		// 验证文件内容格式
		String inputContent = Files.readString(taskPath.resolve("input.md"));
		assertTrue(inputContent.contains("# 文档片段"));
		assertTrue(inputContent.contains("**原始文件:**"));
		assertTrue(inputContent.contains("**任务ID:**"));
		assertTrue(inputContent.contains("## 内容"));
		// 预期input.md格式：标准化的Markdown格式，包含元数据和内容区域

		String statusContent = Files.readString(taskPath.resolve("status.json"));
		assertTrue(statusContent.contains("\"taskId\""));
		assertTrue(statusContent.contains("\"status\":\"pending\""));
		assertTrue(statusContent.contains("\"timestamp\""));
		assertTrue(statusContent.contains("\"inputFile\""));
		// 预期status.json格式：标准化的JSON格式，包含必需的状态字段
	}

	@Test
	void testErrorHandlingRobustness() throws Exception {
		// === 输入准备 - 测试各种错误情况的处理 ===

		// 测试1：空的action参数
		String input1 = """
				{
					"action": "",
					"file_path": "test.txt"
				}
				""";
		ToolExecuteResult result1 = mapReduceTool.run(createMapReduceInput(input1));
		assertNotNull(result1);
		assertTrue(result1.getOutput().contains("未知操作"));
		// 预期输出1：识别为未知操作错误

		// 测试2：null的content参数
		String input2 = """
				{
					"action": "record_map_output",
					"task_id": "test_task",
					"status": "completed"
				}
				""";
		ToolExecuteResult result2 = mapReduceTool.run(createMapReduceInput(input2));
		assertNotNull(result2);
		assertTrue(result2.getOutput().contains("错误：content参数是必需的"));
		// 预期输出2：识别缺少content参数错误

		// 测试3：不存在的任务ID
		String input3 = """
				{
					"action": "record_map_output",
					"content": "test content",
					"task_id": "non_existent_task",
					"status": "completed"
				}
				""";
		ToolExecuteResult result3 = mapReduceTool.run(createMapReduceInput(input3));
		assertNotNull(result3);
		assertTrue(result3.getOutput().contains("错误：任务目录不存在"));
		// 预期输出3：识别任务目录不存在错误

		// 测试4：无效的JSON输入（通过异常处理测试）
		try {
			MapReduceTool.MapReduceInput invalidInput = new MapReduceTool.MapReduceInput();
			invalidInput.setAction("invalid_json_test");
			ToolExecuteResult result4 = mapReduceTool.run(invalidInput);
			assertNotNull(result4);
			// 应该能正常处理，不会抛出异常
		}
		catch (Exception e) {
			fail("工具应该能够处理无效输入而不抛出异常");
		}
		// 预期行为：异常被捕获并转换为错误信息，不会导致程序崩溃
	}

}
