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

package com.alibaba.cloud.ai.manus.planning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.manus.tool.tableProcessor.TableProcessorTool;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.manus.agent.ToolCallbackProvider;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.cron.service.CronService;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.llm.StreamingResponseHandler;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpTool;
import com.alibaba.cloud.ai.manus.mcp.service.McpService;
import com.alibaba.cloud.ai.manus.mcp.service.McpStateHolderService;
import com.alibaba.cloud.ai.manus.planning.service.PlanCreator;
import com.alibaba.cloud.ai.manus.planning.service.PlanFinalizer;
import com.alibaba.cloud.ai.manus.planning.service.IPlanCreator;
import com.alibaba.cloud.ai.manus.planning.service.DynamicAgentPlanCreator;
import com.alibaba.cloud.ai.manus.prompt.service.PromptService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.tool.DocLoaderTool;
import com.alibaba.cloud.ai.manus.tool.FormInputTool;
import com.alibaba.cloud.ai.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.manus.tool.PlanningToolInterface;
import com.alibaba.cloud.ai.manus.tool.DynamicAgentPlanningTool;
import com.alibaba.cloud.ai.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.manus.tool.bash.Bash;
import com.alibaba.cloud.ai.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.manus.tool.database.DatabaseUseTool;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.manus.tool.cron.CronTool;
import com.alibaba.cloud.ai.manus.tool.innerStorage.SmartContentSavingService;
import com.alibaba.cloud.ai.manus.tool.innerStorage.FileMergeTool;
import com.alibaba.cloud.ai.manus.tool.mapreduce.DataSplitTool;
import com.alibaba.cloud.ai.manus.tool.mapreduce.FinalizeTool;
import com.alibaba.cloud.ai.manus.tool.mapreduce.MapOutputTool;
import com.alibaba.cloud.ai.manus.tool.mapreduce.MapReduceSharedStateManager;
import com.alibaba.cloud.ai.manus.tool.mapreduce.ReduceOperationTool;
import com.alibaba.cloud.ai.manus.tool.tableProcessor.TableProcessingService;
import com.alibaba.cloud.ai.manus.tool.textOperator.TextFileOperator;
import com.alibaba.cloud.ai.manus.tool.textOperator.TextFileService;

import com.alibaba.cloud.ai.manus.tool.pptGenerator.PptGeneratorOperator;
import com.alibaba.cloud.ai.manus.tool.jsxGenerator.JsxGeneratorOperator;
import com.alibaba.cloud.ai.manus.tool.excelProcessor.ExcelProcessorTool;
import com.alibaba.cloud.ai.manus.tool.excelProcessor.IExcelProcessingService;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	private final TableProcessingService tableProcessingService;

	private final IExcelProcessingService excelProcessingService;

	private final static Logger log = LoggerFactory.getLogger(PlanningFactory.class);

	private final McpService mcpService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	@Lazy
	private ILlmService llmService;

	@Autowired
	@Lazy
	private ToolCallingManager toolCallingManager;

	@Autowired
	private MapReduceSharedStateManager sharedStateManager;

	@Autowired
	private PromptService promptService;

	@Autowired
	private StreamingResponseHandler streamingResponseHandler;

	@Autowired
	@Lazy
	private CronService cronService;

	@Autowired
	private ISubplanToolService subplanToolService;

	@Autowired
	private AgentService agentService;

	@Autowired
	private PptGeneratorOperator pptGeneratorOperator;

	@Value("${agent.init}")
	private Boolean agentInit = true;

	@Autowired
	private JsxGeneratorOperator jsxGeneratorOperator;

	public PlanningFactory(ChromeDriverService chromeDriverService, PlanExecutionRecorder recorder,
			ManusProperties manusProperties, TextFileService textFileService, McpService mcpService,
			SmartContentSavingService innerStorageService, UnifiedDirectoryManager unifiedDirectoryManager,
			DataSourceService dataSourceService, TableProcessingService tableProcessingService,
			IExcelProcessingService excelProcessingService) {
		this.chromeDriverService = chromeDriverService;
		this.recorder = recorder;
		this.manusProperties = manusProperties;
		this.textFileService = textFileService;
		this.mcpService = mcpService;
		this.innerStorageService = innerStorageService;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.dataSourceService = dataSourceService;
		this.tableProcessingService = tableProcessingService;
		this.excelProcessingService = excelProcessingService;
	}

	/**
	 * Create a plan creator based on plan type
	 * @param planType the type of plan to create ("dynamic_agent" for dynamic agent
	 * plans, any other value for standard plans)
	 * @return configured plan creator instance
	 */
	public IPlanCreator createPlanCreator(String planType) {
		if ("dynamic_agent".equals(planType)) {
			DynamicAgentPlanningTool dynamicAgentPlanningTool = new DynamicAgentPlanningTool();
			return new DynamicAgentPlanCreator(llmService, dynamicAgentPlanningTool, recorder, promptService,
					manusProperties, streamingResponseHandler, agentService);
		}
		else {
			// Get all dynamic agents from the database for simple plans
			List<DynamicAgentEntity> agentEntities = agentService.getAllAgents();
			PlanningToolInterface planningTool = new PlanningTool();
			return new PlanCreator(agentEntities, llmService, planningTool, recorder, promptService, manusProperties,
					streamingResponseHandler);
		}
	}

	/**
	 * Create a PlanFinalizer instance
	 * @return configured PlanFinalizer instance
	 */
	public PlanFinalizer createPlanFinalizer() {
		return new PlanFinalizer(llmService, recorder, promptService, manusProperties, streamingResponseHandler);
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
			String expectedReturnInfo) {
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
		if (agentInit) {
			// Add all tool definitions
			toolDefinitions.add(BrowserUseTool.getInstance(chromeDriverService, innerStorageService, objectMapper));
			toolDefinitions.add(DatabaseUseTool.getInstance(dataSourceService, objectMapper));
			toolDefinitions.add(new TerminateTool(planId, expectedReturnInfo));
			toolDefinitions.add(new Bash(unifiedDirectoryManager, objectMapper));
			toolDefinitions.add(new DocLoaderTool());
			toolDefinitions.add(new TextFileOperator(textFileService, innerStorageService, objectMapper));
			// remove temporarily , because it is hard to test.
			// toolDefinitions.add(new UploadedFileLoaderTool(unifiedDirectoryManager));
			toolDefinitions.add(new TableProcessorTool(tableProcessingService));
			// toolDefinitions.add(new InnerStorageTool(unifiedDirectoryManager));
			// toolDefinitions.add(pptGeneratorOperator);
			// toolDefinitions.add(jsxGeneratorOperator);
			toolDefinitions.add(new FileMergeTool(unifiedDirectoryManager));
			// toolDefinitions.add(new GoogleSearch());
			// toolDefinitions.add(new PythonExecute());
			toolDefinitions.add(new FormInputTool(objectMapper));
			toolDefinitions.add(new DataSplitTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager,
					objectMapper, tableProcessingService));
			toolDefinitions.add(new MapOutputTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager,
					objectMapper));
			toolDefinitions
				.add(new ReduceOperationTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager));
			toolDefinitions.add(new FinalizeTool(planId, manusProperties, sharedStateManager, unifiedDirectoryManager));
			toolDefinitions.add(new CronTool(cronService, objectMapper));
			toolDefinitions.add(new ExcelProcessorTool(excelProcessingService));
		}
		else {
			toolDefinitions.add(new TerminateTool(planId, expectedReturnInfo));
		}

		List<McpServiceEntity> functionCallbacks = mcpService.getFunctionCallbacks(planId);
		for (McpServiceEntity toolCallback : functionCallbacks) {
			String serviceGroup = toolCallback.getServiceGroup();
			ToolCallback[] tCallbacks = toolCallback.getAsyncMcpToolCallbackProvider().getToolCallbacks();
			for (ToolCallback tCallback : tCallbacks) {
				// The serviceGroup is the name of the tool
				toolDefinitions.add(new McpTool(tCallback, serviceGroup, planId, new McpStateHolderService(),
						innerStorageService, objectMapper));
			}
		}
		// Create FunctionToolCallback for each tool
		for (ToolCallBiFunctionDef<?> toolDefinition : toolDefinitions) {
			try {
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
			catch (Exception e) {
				log.error("Failed to register tool: {} - {}", toolDefinition.getName(), e.getMessage(), e);
			}
		}

		// Add subplan tool registration
		if (subplanToolService != null) {
			try {
				Map<String, PlanningFactory.ToolCallBackContext> subplanToolCallbacks = subplanToolService
					.createSubplanToolCallbacks(planId, rootPlanId, expectedReturnInfo);
				toolCallbackMap.putAll(subplanToolCallbacks);
				log.info("Registered {} subplan tools", subplanToolCallbacks.size());
			}
			catch (Exception e) {
				log.warn("Failed to register subplan tools: {}", e.getMessage());
			}
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
