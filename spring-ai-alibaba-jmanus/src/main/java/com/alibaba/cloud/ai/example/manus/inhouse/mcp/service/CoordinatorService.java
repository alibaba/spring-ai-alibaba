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

package com.alibaba.cloud.ai.example.manus.inhouse.mcp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.tool.coordinator.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanConfigVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanParameterVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.McpPlanConfigConverter;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.UserInputWaitState;
import com.alibaba.cloud.ai.example.manus.exception.PlanException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * 协调器服务
 *
 * 负责加载和管理协调器工具，处理业务逻辑
 */
@Service
public class CoordinatorService {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorService.class);

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private UserInputService userInputService;

	private final ObjectMapper objectMapper;

	private final Cache<String, Throwable> exceptionCache;

	public CoordinatorService() {
		this.objectMapper = new ObjectMapper();
		// 注册JSR310模块以支持LocalDateTime等Java 8时间类型
		this.objectMapper.registerModule(new JavaTimeModule());
		// 初始化异常缓存，10分钟过期
		this.exceptionCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
			.build();
	}

	private String convertParametersToSchema(McpPlanConfigVO config) {
		try {
			StringBuilder schema = new StringBuilder();
			schema.append("{\n");
			schema.append("    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
			schema.append("    \"type\": \"object\",\n");
			schema.append("    \"properties\": {\n");

			List<McpPlanParameterVO> parameters = config.getParameters();
			if (parameters != null && !parameters.isEmpty()) {
				for (int i = 0; i < parameters.size(); i++) {
					McpPlanParameterVO param = parameters.get(i);

					if (param != null && param.getName() != null && !param.getName().trim().isEmpty()) {
						schema.append("        \"").append(escapeJsonString(param.getName())).append("\": {\n");
						schema.append("            \"type\": \"").append(convertType(param.getType())).append("\",\n");
						schema.append("            \"description\": \"")
							.append(escapeJsonString(param.getDescription()))
							.append("\"\n");
						schema.append("        }");

						if (i < parameters.size() - 1) {
							schema.append(",");
						}
						schema.append("\n");
					}
				}
			}

			schema.append("    },\n");

			// 添加required字段
			if (parameters != null && !parameters.isEmpty()) {
				List<String> requiredParams = parameters.stream()
					.filter(param -> param != null && param.isRequired() && param.getName() != null
							&& !param.getName().trim().isEmpty())
					.map(McpPlanParameterVO::getName)
					.toList();

				if (!requiredParams.isEmpty()) {
					schema.append("    \"required\": [");
					for (int i = 0; i < requiredParams.size(); i++) {
						schema.append("\"").append(escapeJsonString(requiredParams.get(i))).append("\"");
						if (i < requiredParams.size() - 1) {
							schema.append(", ");
						}
					}
					schema.append("]\n");
				}
				else {
					schema.append("    \"required\": []\n");
				}
			}
			else {
				schema.append("    \"required\": []\n");
			}

			schema.append("}");

			return schema.toString();
		}
		catch (Exception e) {
			log.error("生成JSON Schema时发生错误: {}", e.getMessage(), e);
			// 返回默认的简单Schema
			return """
					{
						"$schema": "http://json-schema.org/draft-07/schema#",
						"type": "object",
						"properties": {},
						"required": []
					}
					""";
		}
	}

	/**
	 * 加载协调器工具
	 * @return 按endpoint分组的协调器工具Map
	 */
	public Map<String, List<CoordinatorTool>> loadCoordinatorTools() {
		log.info("开始加载协调器工具");

		try {
			// 查询服务获取协调器工具列表
			List<CoordinatorTool> coordinatorTools = queryCoordinatorToolsFromService();

			// 按endpoint分组
			Map<String, List<CoordinatorTool>> groupedTools = coordinatorTools.stream()
				.collect(Collectors.groupingBy(CoordinatorTool::getEndpoint));

			log.info("成功加载协调器工具，共 {} 个工具，分组为 {} 个endpoint", coordinatorTools.size(), groupedTools.size());

			return groupedTools;

		}
		catch (Exception e) {
			log.error("加载协调器工具失败: {}", e.getMessage(), e);
			return Map.of();
		}
	}

	/**
	 * 从服务查询协调器工具列表
	 * @return 协调器工具列表
	 */
	private List<CoordinatorTool> queryCoordinatorToolsFromService() {
		log.debug("查询协调器工具服务");

		List<CoordinatorTool> tools = new ArrayList<>();

		try {
			// 从Spring容器获取所有CoordinatorTool实例
			Map<String, CoordinatorTool> coordinatorToolBeans = applicationContext
				.getBeansOfType(CoordinatorTool.class);

			if (coordinatorToolBeans.isEmpty()) {
				log.info("未找到CoordinatorTool实例，创建示例数据");
				// 创建示例数据用于测试
				tools = createExampleCoordinatorTools();
			}
			else {
				log.info("从Spring容器获取到 {} 个CoordinatorTool实例", coordinatorToolBeans.size());
				tools.addAll(coordinatorToolBeans.values());
			}
		}
		catch (Exception e) {
			log.warn("获取协调器工具时出现异常: {}", e.getMessage());
			// 创建示例数据作为fallback
			tools = createExampleCoordinatorTools();
		}

		return tools;
	}

	/**
	 * 创建示例协调器工具（用于测试）
	 * @return 示例协调器工具列表
	 */
	private List<CoordinatorTool> createExampleCoordinatorTools() {
		List<CoordinatorTool> tools = new ArrayList<>();
		
		try {
			// 创建示例计划配置
			String planJson = """
					{
					    "planId": "planTemplate-1754276365157",
					    "title": "Plan for retrieving and saving Alibaba's stock information",
					    "userRequest": "打开百度查询某公司最近一周的股票。生成markdown文件到本地",
					    "steps": [
					        "[BROWSER_AGENT] Search for {company} stock information for the last {period}",
					        "[DEFAULT_AGENT] Save the searched information into a {fileType} file"
					    ]
					}
					""";
			
			McpPlanConfigConverter converter = new McpPlanConfigConverter();
			McpPlanConfigVO config = converter.convert(planJson);
			
			// 使用convertToCoordinatorTool方法转换
			CoordinatorTool tool = convertToCoordinatorTool(config);
			if (tool != null) {
				// 设置不同的endpoint用于测试
				tool.setEndpoint("/mcp/coordinator-example");
				tools.add(tool);
				log.info("成功创建示例CoordinatorTool: {}", tool.getToolName());
			}
			
			// 创建第二个示例
			String planJson2 = """
					{
					    "planId": "planTemplate-1754276365158",
					    "title": "Data Analysis Plan",
					    "userRequest": "分析用户数据并生成报告",
					    "steps": [
					        "[DATA_AGENT] Analyze {dataSource} for {timeRange}",
					        "[REPORT_AGENT] Generate {reportType} report"
					    ]
					}
					""";
			
			McpPlanConfigVO config2 = converter.convert(planJson2);
			CoordinatorTool tool2 = convertToCoordinatorTool(config2);
			if (tool2 != null) {
				tool2.setEndpoint("/mcp/coordinator-analysis");
				tools.add(tool2);
				log.info("成功创建第二个示例CoordinatorTool: {}", tool2.getToolName());
			}
			
		}
		catch (Exception e) {
			log.warn("创建示例协调器工具时出现异常: {}", e.getMessage());
		}

		log.info("成功创建 {} 个示例CoordinatorTool", tools.size());
		return tools;
	}

	/**
	 * 为协调器工具创建工具规范
	 * @param tool 协调器工具
	 * @return 工具规范
	 */
	public McpServerFeatures.SyncToolSpecification createToolSpecification(CoordinatorTool tool) {
		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(io.modelcontextprotocol.spec.McpSchema.Tool.builder()
				.name(tool.getToolName())
				.description(tool.getToolDescription())
				.inputSchema(tool.getToolSchema())
				.build())
			.callHandler((exchange, request) -> invokeTool(request))
			.build();
	}

	/**
	 * 调用协调器工具
	 * @param request 工具调用请求
	 * @return 工具调用结果
	 */
	public CallToolResult invokeTool(CallToolRequest request) {
		try {
			log.debug("调用计划协调工具，参数: {}", request.arguments());
			String resultString = null;
			String toolName = request.name();

			// 将参数转换为JSON字符串
			String rawParam = objectMapper.writeValueAsString(request.arguments());

			log.info("执行计划模板: {}, 参数: {}", toolName, rawParam);

			// 调用计划模板服务
			ResponseEntity<Map<String, Object>> responseEntity = planTemplateService
				.executePlanByTemplateIdInternal(toolName, rawParam);

			// 创建简化的返回结果
			CoordinatorResult response;

			// 处理服务返回结果
			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				Map<String, Object> executionResult = responseEntity.getBody();

				// 获取planId和状态
				String planId = (String) executionResult.get("planId");
				String status = (String) executionResult.get("status");
				String message = (String) executionResult.get("message");
				resultString = pollPlanExecutionStatus(planId);
				response = new CoordinatorResult(planId, request.arguments(), 2, "success", resultString);

			}
			else {
				response = new CoordinatorResult(null, request.arguments(), 2, "failed", resultString);
			}

			// 转换为JSON字符串
			String resultJson = objectMapper.writeValueAsString(response);

			log.info("计划模板执行完成: {}, 结果: {}", toolName, resultJson);

			// 直接返回结果字符串，与其他工具保持一致
			return new CallToolResult(List.of(new McpSchema.TextContent(resultJson)), null);

		}
		catch (Exception e) {
			log.error("计划协调工具调用失败: {}", e.getMessage(), e);

			// 创建简化的错误响应
			CoordinatorResult errorResponse = new CoordinatorResult(null, request.arguments(), 2, "error", null);
			try {
				String errorJson = objectMapper.writeValueAsString(errorResponse);
				return new CallToolResult(List.of(new McpSchema.TextContent(errorJson)), null);
			}
			catch (Exception jsonError) {
				return new CallToolResult(List.of(new McpSchema.TextContent("计划协调工具调用失败: " + e.getMessage())), null);
			}
		}
	}

	/**
	 * 获取结果输出
	 */
	public String getResultOutput(PlanExecutionRecord record) {
		// 参数验证
		if (record == null) {
			log.warn("PlanExecutionRecord为空，无法获取结果输出");
			return "PlanExecutionRecord为空，无法获取结果输出";
		}

		List<AgentExecutionRecord> sequence = record.getAgentExecutionSequence();
		if (sequence == null || sequence.isEmpty()) {
			log.debug("Agent执行序列为空，无法获取结果输出");
			return "Agent执行序列为空，无法获取结果输出";
		}

		try {
			// 获取最后一个Agent执行记录
			AgentExecutionRecord lastAgentRecord = sequence.get(sequence.size() - 1);
			List<ThinkActRecord> thinkActSteps = lastAgentRecord.getThinkActSteps();

			if (thinkActSteps == null || thinkActSteps.isEmpty()) {
				log.debug("ThinkAct步骤为空，无法获取结果输出");
				return "ThinkAct步骤为空，无法获取结果输出";
			}

			// 获取最后一个ThinkAct记录
			ThinkActRecord lastThinkActRecord = thinkActSteps.get(thinkActSteps.size() - 1);
			List<ThinkActRecord.ActToolInfo> actToolInfoList = lastThinkActRecord.getActToolInfoList();

			if (actToolInfoList == null || actToolInfoList.isEmpty()) {
				log.debug("ActTool信息列表为空，无法获取结果输出");
				return "ActTool信息列表为空，无法获取结果输出";
			}

			// 获取最后一个工具调用的结果
			ThinkActRecord.ActToolInfo lastToolInfo = actToolInfoList.get(actToolInfoList.size() - 1);
			String result = lastToolInfo.getResult();

			if (result == null) {
				log.debug("工具调用结果为空");
				return "工具调用结果为空";
			}

			log.debug("成功获取结果输出: {}", result);
			return result;

		}
		catch (IndexOutOfBoundsException e) {
			log.error("访问执行记录时发生索引越界异常: {}", e.getMessage(), e);
			return null;
		}
		catch (Exception e) {
			log.error("获取结果输出时发生异常: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 轮询计划执行状态
	 * @param planId 计划ID
	 * @return 执行结果字符串
	 */
	public String pollPlanExecutionStatus(String planId) {
		if (planId == null || planId.trim().isEmpty()) {
			log.warn("计划ID为空，无法进行轮询");
			return null;
		}

		log.info("开始轮询计划执行状态: {}", planId);

		try {
			// 开始轮询，直到计划完成
			PlanExecutionRecord record = pollPlanUntilCompleted(planId);

			// 调用getResultOutput方法获取最终结果字符串
			String resultOutput = getResultOutput(record);

			if (resultOutput != null) {
				log.info("成功获取计划执行结果: {}", resultOutput);
				return resultOutput;
			}
			else {
				log.warn("无法获取计划执行结果，返回默认信息");
				return "计划执行完成，但未获取到具体结果";
			}
		}
		catch (Exception e) {
			log.error("轮询计划执行状态失败: {}", e.getMessage(), e);
			return "计划执行失败: " + e.getMessage();
		}
	}

	/**
	 * 轮询计划直到完成
	 * @param planId 计划ID
	 * @return 完成后的计划执行记录
	 */
	private PlanExecutionRecord pollPlanUntilCompleted(String planId) {
		int maxAttempts = 60; // 最大轮询次数（10分钟）
		int attempt = 0;
		long pollInterval = 10000; // 轮询间隔10秒

		while (attempt < maxAttempts) {
			attempt++;
			log.debug("轮询计划 {} 第 {} 次尝试", planId, attempt);

			try {
				// 获取执行记录
				PlanExecutionRecord planRecord = getPlanExecutionRecord(planId);

				if (planRecord != null) {
					log.debug("获取到计划 {} 的执行记录", planId);

					// 检查是否完成
					if (isPlanCompleted(planRecord)) {
						log.info("计划 {} 已完成，轮询结束", planId);
						return planRecord;
					}
					else {
						log.debug("计划 {} 尚未完成，继续轮询", planId);
					}
				}
				else {
					log.warn("计划 {} 不存在", planId);
					throw new RuntimeException("Plan not found: " + planId);
				}

				// 等待指定时间后继续轮询
				if (attempt < maxAttempts) {
					log.debug("等待 {} 毫秒后继续轮询", pollInterval);
					Thread.sleep(pollInterval);
				}
			}
			catch (InterruptedException e) {
				log.warn("轮询被中断: {}", e.getMessage());
				Thread.currentThread().interrupt();
				throw new RuntimeException("Polling interrupted: " + e.getMessage());
			}
			catch (Exception e) {
				log.error("轮询过程中发生错误: {}", e.getMessage(), e);
				// 继续尝试，不立即返回错误
			}
		}

		log.warn("计划 {} 轮询超时，已达到最大尝试次数 {}", planId, maxAttempts);
		throw new RuntimeException("Polling timeout after " + maxAttempts + " attempts");
	}

	/**
	 * 获取计划执行记录
	 * @param planId 计划ID
	 * @return 计划执行记录
	 */
	private PlanExecutionRecord getPlanExecutionRecord(String planId) {
		try {
			// 检查异常缓存
			Throwable throwable = this.exceptionCache.getIfPresent(planId);
			if (throwable != null) {
				throw new PlanException(throwable);
			}

			// 获取执行记录
			PlanExecutionRecord planRecord = planExecutionRecorder.getRootPlanExecutionRecord(planId);

			if (planRecord == null) {
				return null;
			}

			// 检查用户输入等待状态并合并到计划记录中
			UserInputWaitState waitState = userInputService.getWaitState(planId);
			if (waitState != null && waitState.isWaiting()) {
				planRecord.setUserInputWaitState(waitState);
				log.debug("计划 {} 正在等待用户输入，已将等待状态合并到详情响应中", planId);
			}
			else {
				planRecord.setUserInputWaitState(null); // 如果不等待则清除
			}

			return planRecord;
		}
		catch (PlanException e) {
			log.error("计划执行异常: {}", e.getMessage(), e);
			throw new RuntimeException("Plan execution exception: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("获取执行记录失败: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get execution record: " + e.getMessage());
		}
	}

	/**
	 * 检查计划是否完成
	 * @param planRecord 计划执行记录
	 * @return 是否完成
	 */
	private boolean isPlanCompleted(PlanExecutionRecord planRecord) {
		if (planRecord == null) {
			return false;
		}

		// 检查 completed 字段
		if (planRecord.isCompleted()) {
			return true;
		}

		// 检查是否有结束时间
		if (planRecord.getEndTime() != null) {
			return true;
		}

		// 检查用户输入等待状态
		if (planRecord.getUserInputWaitState() != null && planRecord.getUserInputWaitState().isWaiting()) {
			return false; // 等待用户输入时不算完成
		}

		return false;
	}

	/**
	 * 转义JSON字符串
	 * @param input 输入字符串
	 * @return 转义后的字符串
	 */
	private String escapeJsonString(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	/**
	 * 转换参数类型为JSON Schema类型
	 * @param type 原始类型
	 * @return JSON Schema类型
	 */
	private String convertType(String type) {
		if (type == null || type.trim().isEmpty()) {
			return "string";
		}

		String lowerType = type.toLowerCase().trim();
		switch (lowerType) {
			case "int":
			case "integer":
			case "number":
				return "number";
			case "boolean":
			case "bool":
				return "boolean";
			case "array":
			case "list":
				return "array";
			case "object":
			case "map":
				return "object";
			default:
				return "string";
		}
	}

	/**
	 * 将McpPlanConfigVO转换为CoordinatorTool
	 * @param config McpPlanConfigVO配置对象
	 * @return CoordinatorTool对象
	 */
	public CoordinatorTool convertToCoordinatorTool(McpPlanConfigVO config) {
		if (config == null) {
			log.warn("McpPlanConfigVO为空，无法转换为CoordinatorTool");
			return null;
		}

		CoordinatorTool tool = new CoordinatorTool();

		// 设置endpoint默认为example
		tool.setEndpoint("example");

		// 将config的各个属性赋值给tool
		if (config.getName() != null && !config.getName().trim().isEmpty()) {
			tool.setToolName(config.getId());
		}

		if (config.getDescription() != null && !config.getDescription().trim().isEmpty()) {
			tool.setToolDescription(config.getDescription());
		}

		// 使用convertParametersToSchema转换参数并设置给tool
		String schema = convertParametersToSchema(config);
		tool.setToolSchema(schema);

		log.debug("成功将McpPlanConfigVO转换为CoordinatorTool: {}", tool.getToolName());
		return tool;
	}

	/**
	 * 批量转换McpPlanConfigVO列表为CoordinatorTool列表
	 * @param configs McpPlanConfigVO配置列表
	 * @return CoordinatorTool列表
	 */
	public List<CoordinatorTool> convertToCoordinatorTools(List<McpPlanConfigVO> configs) {
		if (configs == null || configs.isEmpty()) {
			log.warn("McpPlanConfigVO列表为空，无法转换");
			return new ArrayList<>();
		}

		List<CoordinatorTool> tools = new ArrayList<>();
		for (McpPlanConfigVO config : configs) {
			CoordinatorTool tool = convertToCoordinatorTool(config);
			if (tool != null) {
				tools.add(tool);
			}
		}

		log.info("成功转换 {} 个McpPlanConfigVO为CoordinatorTool", tools.size());
		return tools;
	}

}
