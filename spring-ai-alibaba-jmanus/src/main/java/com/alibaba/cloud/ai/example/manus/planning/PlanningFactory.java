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

package com.alibaba.cloud.ai.example.manus.planning;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpTool;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpService;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpStateHolderService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
import com.alibaba.cloud.ai.example.manus.planning.executor.MapReducePlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.DocLoaderTool;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;
import com.alibaba.cloud.ai.example.manus.tool.MapReducePlanningTool;
import com.alibaba.cloud.ai.example.manus.tool.PlanningToolInterface;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.bash.Bash;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.code.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageService;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageTool;
import com.alibaba.cloud.ai.example.manus.tool.searchAPI.GoogleSearch;
import com.alibaba.cloud.ai.example.manus.tool.split.SplitTool;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileOperator;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileService;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class PlanningFactory {

	private final ChromeDriverService chromeDriverService;

	private final PlanExecutionRecorder recorder;

	private final ManusProperties manusProperties;

	private final TextFileService textFileService;

	private final InnerStorageService innerStorageService;

	@Autowired
	private AgentService agentService;

	private final McpService mcpService;

	@Autowired
	@Lazy
	private LlmService llmService;

	@Autowired
	@Lazy
	private ToolCallingManager toolCallingManager;

	@Autowired
	private DynamicAgentLoader dynamicAgentLoader;

	@Autowired
	private McpStateHolderService mcpStateHolderService;

	@Autowired
	@Lazy
	private SummaryWorkflow summaryWorkflow;

	public PlanningFactory(ChromeDriverService chromeDriverService, PlanExecutionRecorder recorder,
			ManusProperties manusProperties, TextFileService textFileService, McpService mcpService,
			InnerStorageService innerStorageService) {
		this.chromeDriverService = chromeDriverService;
		this.recorder = recorder;
		this.manusProperties = manusProperties;
		this.textFileService = textFileService;
		this.mcpService = mcpService;
		this.innerStorageService = innerStorageService;
	}

	//这里修改Mapreduce的逻辑
	public PlanningCoordinator createPlanningCoordinator(String planId) {

		// Add all dynamic agents from the database
		List<DynamicAgentEntity> agentEntities = dynamicAgentLoader.getAllAgents();

		PlanningToolInterface planningTool = new MapReducePlanningTool();

		PlanCreator planCreator = new PlanCreator(agentEntities, llmService, planningTool, recorder);
		PlanExecutorInterface planExecutor = new MapReducePlanExecutor(agentEntities, recorder, agentService, llmService);
		PlanFinalizer planFinalizer = new PlanFinalizer(llmService, recorder);

		PlanningCoordinator planningCoordinator = new PlanningCoordinator(planCreator, planExecutor, planFinalizer);

		return planningCoordinator;
	}

	public static class ToolCallBackContext {

		private final ToolCallback toolCallback;

		private final ToolCallBiFunctionDef functionInstance;

		public ToolCallBackContext(ToolCallback toolCallback, ToolCallBiFunctionDef functionInstance) {
			this.toolCallback = toolCallback;
			this.functionInstance = functionInstance;
		}

		public ToolCallback getToolCallback() {
			return toolCallback;
		}

		public ToolCallBiFunctionDef getFunctionInstance() {
			return functionInstance;
		}

	}

	public Map<String, ToolCallBackContext> toolCallbackMap(String planId) {
		Map<String, ToolCallBackContext> toolCallbackMap = new HashMap<>();
		List<ToolCallBiFunctionDef> toolDefinitions = new ArrayList<>();

		// 添加所有工具定义
		toolDefinitions.add(BrowserUseTool.getInstance(chromeDriverService, innerStorageService));
		toolDefinitions.add(new TerminateTool(planId));
		toolDefinitions.add(new Bash(manusProperties));
		toolDefinitions.add(new DocLoaderTool());
		toolDefinitions.add(new TextFileOperator(textFileService));
		toolDefinitions.add(new InnerStorageTool(innerStorageService, summaryWorkflow));
		toolDefinitions.add(new GoogleSearch());
		toolDefinitions.add(new PythonExecute());
		toolDefinitions.add(new FormInputTool());
		toolDefinitions.add(new SplitTool(planId));
		List<McpServiceEntity> functionCallbacks = mcpService.getFunctionCallbacks(planId);
		for (McpServiceEntity toolCallback : functionCallbacks) {
			String serviceGroup = toolCallback.getServiceGroup();
			ToolCallback[] tCallbacks = toolCallback.getAsyncMcpToolCallbackProvider().getToolCallbacks();
			for (ToolCallback tCallback : tCallbacks) {
				// 这里的 serviceGroup 是工具的名称
				toolDefinitions.add(new McpTool(tCallback, serviceGroup, planId, mcpStateHolderService));
			}
		}

		// 为每个工具创建 FunctionToolCallback
		for (ToolCallBiFunctionDef toolDefinition : toolDefinitions) {
			FunctionToolCallback<?, ?> functionToolcallback = FunctionToolCallback
				.builder(toolDefinition.getName(), toolDefinition)
				.description(toolDefinition.getDescription())
				.inputSchema(toolDefinition.getParameters())
				.inputType(toolDefinition.getInputType())
				.toolMetadata(ToolMetadata.builder().returnDirect(toolDefinition.isReturnDirect()).build())
				.build();
			toolDefinition.setPlanId(planId);
			ToolCallBackContext functionToolcallbackContext = new ToolCallBackContext(functionToolcallback,
					toolDefinition);
			toolCallbackMap.put(toolDefinition.getName(), functionToolcallbackContext);
		}
		return toolCallbackMap;
	}

	@Bean
	public RestClient.Builder createRestClient() {
		// 创建 RequestConfig 并设置超时
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES)) // 设置连接超时
			.setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.build();

		// 3. 创建 CloseableHttpClient 并应用配置
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

		// 4. 使用 HttpComponentsClientHttpRequestFactory 包装 HttpClient
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		// 5. 创建 RestClient 并设置请求工厂
		return RestClient.builder().requestFactory(requestFactory);
	}

	/**
	 * Provides an empty ToolCallbackProvider implementation when MCP is disabled
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "false")
	public ToolCallbackProvider emptyToolCallbackProvider() {
		return () -> new ToolCallback[0];
	}

}
