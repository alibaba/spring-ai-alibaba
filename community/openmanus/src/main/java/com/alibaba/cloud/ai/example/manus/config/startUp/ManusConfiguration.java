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

package com.alibaba.cloud.ai.example.manus.config.startUp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.DynamicAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.DocLoaderTool;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.bash.Bash;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.alibaba.cloud.ai.example.manus.tool.code.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.searchAPI.GoogleSearch;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileOperator;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileService;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Configuration
public class ManusConfiguration {

	private final ChromeDriverService chromeDriverService;

	private final PlanExecutionRecorder recorder;

	private final ManusProperties manusProperties;

	private final TextFileService textFileService;

	public ManusConfiguration(ChromeDriverService chromeDriverService, PlanExecutionRecorder recorder,
			ManusProperties manusProperties, TextFileService textFileService) {
		this.chromeDriverService = chromeDriverService;
		this.recorder = recorder;
		this.manusProperties = manusProperties;
		this.textFileService = textFileService;
	}

	// @Bean
	// @Scope("prototype") // 每次请求创建一个新的实例
	// public PlanningFlow planningFlow(LlmService llmService, ToolCallingManager
	// toolCallingManager) {

	// ManusAgent manusAgent = new ManusAgent(llmService, toolCallingManager,
	// chromeDriverService,
	// CodeUtils.WORKING_DIR, recorder, manusProperties);
	// BrowserAgent browserAgent = new BrowserAgent(llmService, toolCallingManager,
	// chromeDriverService, recorder, manusProperties);

	// FileAgent fileAgent = new FileAgent(llmService, toolCallingManager,
	// CodeUtils.WORKING_DIR, recorder, manusProperties);
	// PythonAgent pythonAgent = new PythonAgent(llmService, toolCallingManager,
	// CodeUtils.WORKING_DIR, recorder, manusProperties);

	// List<BaseAgent> agentList = new ArrayList<>();

	// agentList.add(manusAgent);
	// agentList.add(browserAgent);
	// agentList.add(fileAgent);
	// agentList.add(pythonAgent);

	// Map<String, Object> data = new HashMap<>();
	// return new PlanningFlow(agentList, data, recorder);
	// }

	@Bean
	@Scope("prototype")
	public PlanningFlow planningFlow(LlmService llmService, ToolCallingManager toolCallingManager,
			DynamicAgentLoader dynamicAgentLoader) {
		List<BaseAgent> agentList = new ArrayList<>();
		Map<String, ToolCallBackContext> toolCallbackMap = new HashMap<>();
		// Add all dynamic agents from the database
		for (DynamicAgentEntity agentEntity : dynamicAgentLoader.getAllAgents()) {
			DynamicAgent agent = dynamicAgentLoader.loadAgent(agentEntity.getAgentName());
			toolCallbackMap = toolCallbackMap(agent);
			agent.setToolCallbackMap(toolCallbackMap);
			agentList.add(agent);
		}

		Map<String, Object> data = new HashMap<>();
		// hack 暂时不想让planning flow 也继承agent，所以用了个讨巧的办法，以后要改掉， 这个讨巧办法的前提假设是tools
		// 只调用baseagent的getPlanId方法
		return new PlanningFlow(agentList, data, recorder, toolCallbackMap);
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

	public Map<String, ToolCallBackContext> toolCallbackMap(BaseAgent agent) {
		Map<String, ToolCallBackContext> toolCallbackMap = new HashMap<>();
		List<ToolCallBiFunctionDef> toolDefinitions = new ArrayList<>();

		// 添加所有工具定义
		toolDefinitions.add(BrowserUseTool.getInstance(chromeDriverService));
		toolDefinitions.add(new TerminateTool(null));
		toolDefinitions.add(new Bash(CodeUtils.WORKING_DIR));
		toolDefinitions.add(new DocLoaderTool());
		toolDefinitions.add(new TextFileOperator(CodeUtils.WORKING_DIR, textFileService));
		toolDefinitions.add(new GoogleSearch());
		toolDefinitions.add(new PythonExecute());

		// 为每个工具创建 FunctionToolCallback
		for (ToolCallBiFunctionDef toolDefinition : toolDefinitions) {
			FunctionToolCallback functionToolcallback = FunctionToolCallback
				.builder(toolDefinition.getName(), toolDefinition)
				.description(toolDefinition.getDescription())
				.inputSchema(toolDefinition.getParameters())
				.inputType(toolDefinition.getInputType())
				.toolMetadata(ToolMetadata.builder().returnDirect(toolDefinition.isReturnDirect()).build())
				.build();
			toolDefinition.setAgent(agent);
			ToolCallBackContext functionToolcallbackContext = new ToolCallBackContext(functionToolcallback,
					toolDefinition);
			toolCallbackMap.put(toolDefinition.getName(), functionToolcallbackContext);
		}
		return toolCallbackMap;
	}

	/**
	 * PlanningFlowManager 为了与controller等方法兼容 ，并且还能保证每次请求都能创建一个新的PlanningFlow实例，来解决并发问题。
	 */
	@Component
	public class PlanningFlowManager {

		private final ApplicationContext context;

		private ConcurrentHashMap<String, PlanningFlow> flowMap = new ConcurrentHashMap<>();

		public PlanningFlowManager(ApplicationContext context) {
			this.context = context;
		}

		public PlanningFlow getOrCreatePlanningFlow(String requestId) {
			PlanningFlow flow = flowMap.computeIfAbsent(requestId, key -> {
				PlanningFlow newFlow = context.getBean(PlanningFlow.class);
				newFlow.setActivePlanId(key);
				return newFlow;
			});
			return flow;
		}

		public boolean removePlanningFlow(String requestId) {
			return flowMap.remove(requestId) != null;
		}

	}

	@Bean
	public RestClient.Builder createRestClient() {
		// 1. 配置超时时间（单位：毫秒）
		int connectionTimeout = 600000; // 连接超时时间
		int readTimeout = 600000; // 响应读取超时时间
		int writeTimeout = 600000; // 请求写入超时时间

		// 2. 创建 RequestConfig 并设置超时
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
