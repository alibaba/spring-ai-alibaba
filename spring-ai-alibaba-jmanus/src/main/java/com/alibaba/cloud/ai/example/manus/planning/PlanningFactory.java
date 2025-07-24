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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.ToolCallbackProvider;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.service.CronService;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.IDynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpTool;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpService;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpStateHolderService;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.llm.StreamingResponseHandler;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.creator.PlanCreator;
import com.alibaba.cloud.ai.example.manus.planning.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.example.manus.planning.finalizer.PlanFinalizer;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.DocLoaderTool;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus.tool.PlanningToolInterface;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.bash.Bash;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseUseTool;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.tool.cron.CronTool;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageContentTool;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.FileMergeTool;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.DataSplitTool;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.FinalizeTool;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.MapOutputTool;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.MapReduceSharedStateManager;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.ReduceOperationTool;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileOperator;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.TextFileService;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class PlanningFactory implements IPlanningFactory {

	private final ChromeDriverService chromeDriverService;

	private final PlanExecutionRecorder recorder;

	private final ManusProperties manusProperties;

	private final TextFileService textFileService;

	private final SmartContentSavingService innerStorageService;

	private final UnifiedDirectoryManager unifiedDirectoryManager;

	private final DataSourceService dataSourceService;

	private final static Logger log = LoggerFactory.getLogger(PlanningFactory.class);

	@Autowired
	private AgentService agentService;

	private final McpService mcpService;

	@Autowired
	@Lazy
	private ILlmService llmService;

	@Autowired
	@Lazy
	private ToolCallingManager toolCallingManager;

	@Autowired
	private IDynamicAgentLoader dynamicAgentLoader;

	@Autowired
	private MapReduceSharedStateManager sharedStateManager;

	@Autowired
	@Lazy
	private SummaryWorkflow summaryWorkflow;

	@Autowired
	@Lazy
	private PlanExecutorFactory planExecutorFactory;

	@Autowired
	private PromptService promptService;

	@Autowired
	private StreamingResponseHandler streamingResponseHandler;

	@Autowired
	private CronService cronService;

	public PlanningFactory(ChromeDriverService chromeDriverService, PlanExecutionRecorder recorder,
			ManusProperties manusProperties, TextFileService textFileService, McpService mcpService,
			SmartContentSavingService innerStorageService, UnifiedDirectoryManager unifiedDirectoryManager,
			DataSourceService dataSourceService) {
		this.chromeDriverService = chromeDriverService;
		this.recorder = recorder;
		this.manusProperties = manusProperties;
		this.textFileService = textFileService;
		this.mcpService = mcpService;
		this.innerStorageService = innerStorageService;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.dataSourceService = dataSourceService;
	}

	// Use the enhanced PlanningCoordinator with dynamic executor selection
	public PlanningCoordinator createPlanningCoordinator(String planId) {

		// Add all dynamic agents from the database
		List<DynamicAgentEntity> agentEntities = dynamicAgentLoader.getAllAgents();

		PlanningToolInterface planningTool = new PlanningTool();

		PlanCreator planCreator = new PlanCreator(agentEntities, llmService, planningTool, recorder, promptService,
				manusProperties, streamingResponseHandler);

		PlanFinalizer planFinalizer = new PlanFinalizer(llmService, recorder, promptService, manusProperties,
				streamingResponseHandler);

		PlanningCoordinator planningCoordinator = new PlanningCoordinator(planCreator, planExecutorFactory,
				planFinalizer);

		return planningCoordinator;
	}

	public static class ToolCallBackContext {

		private final ToolCallback toolCallback;

		private final ToolCallBiFunctionDef<?> functionInstance;

		public ToolCallBackContext(ToolCallback toolCallback, ToolCallBiFunctionDef<?> functionInstance) {
			this.toolCallback = toolCallback;
			this.functionInstance = functionInstance;
		}

		public ToolCallback getToolCallback() {
			return toolCallback;
		}

		public ToolCallBiFunctionDef<?> getFunctionInstance() {
			return functionInstance;
		}

	}

	public Map<String, ToolCallBackContext> toolCallbackMap(String planId, String rootPlanId,
			List<String> terminateColumns) {
		Map<String, ToolCallBackContext> toolCallbackMap = new HashMap<>();
		List<ToolCallBiFunctionDef<?>> toolDefinitions = new ArrayList<>();
		if (chromeDriverService == null) {
			log.error("ChromeDriverService is null, skipping BrowserUseTool registration");
			return toolCallbackMap;
		}
		if (innerStorageService == null) {
			log.error("SmartContentSavingService is null, skipping BrowserUseTool registration");
			return toolCallbackMap;
		}
		// Add all tool definitions
		toolDefinitions.add(BrowserUseTool.getInstance(chromeDriverService, innerStorageService));
		toolDefinitions.add(DatabaseUseTool.getInstance(dataSourceService));
		toolDefinitions.add(new TerminateTool(planId, terminateColumns));
		toolDefinitions.add(new Bash(unifiedDirectoryManager));
		toolDefinitions.add(new DocLoaderTool());
		toolDefinitions.add(new TextFileOperator(textFileService, innerStorageService));
		// toolDefinitions.add(new InnerStorageTool(unifiedDirectoryManager));
		toolDefinitions.add(new InnerStorageContentTool(unifiedDirectoryManager, summaryWorkflow, recorder));
		toolDefinitions.add(new FileMergeTool(unifiedDirectoryManager));
		// toolDefinitions.add(new GoogleSearch());
		// toolDefinitions.add(new PythonExecute());
		toolDefinitions.add(new FormInputTool());
		toolDefinitions.add(new DataSplitTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager));
		toolDefinitions.add(new MapOutputTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager,
				terminateColumns));
		toolDefinitions.add(new ReduceOperationTool(planId, manusProperties, sharedStateManager,
				unifiedDirectoryManager, terminateColumns));
		toolDefinitions.add(new FinalizeTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager));
		toolDefinitions.add(new CronTool(cronService));

		List<McpServiceEntity> functionCallbacks = mcpService.getFunctionCallbacks(planId);
		for (McpServiceEntity toolCallback : functionCallbacks) {
			String serviceGroup = toolCallback.getServiceGroup();
			ToolCallback[] tCallbacks = toolCallback.getAsyncMcpToolCallbackProvider().getToolCallbacks();
			for (ToolCallback tCallback : tCallbacks) {
				// The serviceGroup is the name of the tool
				toolDefinitions.add(
						new McpTool(tCallback, serviceGroup, planId, new McpStateHolderService(), innerStorageService));
			}
		}

		// Create FunctionToolCallback for each tool
		for (ToolCallBiFunctionDef<?> toolDefinition : toolDefinitions) {
			FunctionToolCallback<?, ToolExecuteResult> functionToolcallback = FunctionToolCallback
				.builder(toolDefinition.getName(), toolDefinition)
				.description(toolDefinition.getDescription())
				.inputSchema(toolDefinition.getParameters())
				.inputType(toolDefinition.getInputType())
				.toolMetadata(ToolMetadata.builder().returnDirect(toolDefinition.isReturnDirect()).build())
				.build();
			toolDefinition.setCurrentPlanId(planId);
			toolDefinition.setRootPlanId(rootPlanId);
			log.info("Registering tool: {}", toolDefinition.getName());
			ToolCallBackContext functionToolcallbackContext = new ToolCallBackContext(functionToolcallback,
					toolDefinition);
			toolCallbackMap.put(toolDefinition.getName(), functionToolcallbackContext);
		}
		return toolCallbackMap;
	}

	@Bean
	public RestClient.Builder createRestClient() {
		// Create RequestConfig and set the timeout (10 minutes for all timeouts)
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES)) // Set the connection
																	// timeout
			.setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.build();

		// Create CloseableHttpClient and apply the configuration
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

		// Use HttpComponentsClientHttpRequestFactory to wrap HttpClient
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		// Create RestClient and set the request factory
		return RestClient.builder().requestFactory(requestFactory);
	}

	/**
	 * Provides an empty ToolCallbackProvider implementation when MCP is disabled
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "false")
	public ToolCallbackProvider emptyToolCallbackProvider() {
		return () -> new HashMap<String, PlanningFactory.ToolCallBackContext>();
	}

}
