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
package com.alibaba.cloud.ai.example.manus.workflow;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MapReduce-based content summarization workflow for intelligent extraction and
 * structured summarization of large amounts of content
 */
@Component
public class SummaryWorkflow implements ISummaryWorkflow {

	private static final Logger logger = LoggerFactory.getLogger(SummaryWorkflow.class);

	@Autowired
	private PlanningFactory planningFactory;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PromptService promptService;

	/**
	 * Get summary plan template directly as built-in English version
	 *
	 * We use a local template instead of PromptService for the following reasons: 1. This
	 * is a core workflow template that should not be externally modified 2. Using a local
	 * template improves performance by avoiding service calls 3. It ensures consistency
	 * and stability of the core summarization workflow 4. The template is fixed and does
	 * not require dynamic configuration
	 */
	private String getSummaryPlanTemplate() {
		return """
				{
				  "planType": "advanced",
				  "planId": "<planId>",
				  "title": "Intelligent content summarization for large files, with final merged file name output in summary",
				  "steps": [
				    {
				      "type": "mapreduce",
				      "dataPreparedSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_DATA_PREPARE_AGENT] Use map_reduce_tool to split content of file <fileName>"
				        }
				      ],
				      "mapSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_MAP_TASK_AGENT] Analyze file, find key information related to ```<queryKey>```, information should be comprehensive, including all data, facts and opinions, comprehensive information without omission. Output format specification: ``` <outputFormatSpecification>```. File format requirement: Markdown."
				        }
				      ],
				      "reduceSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_REDUCE_TASK_AGENT] Merge the information from this chunk into file, while maintaining information integrity, merge all content and remove results with no content found. Output format specification: <outputFormatSpecification>. File format requirement: Markdown."
				        }
				      ],
				      "postProcessSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_FIN_AGENT] After export completion, read the exported results and output all exported content completely. Output format specification: <outputFormatSpecification>. File format requirement: Markdown."
				        }
				      ]
				    }
				  ]
				}""";
	}

	/**
	 * Execute content summarization workflow
	 * @param planId Caller's plan ID to ensure subprocess can find corresponding
	 * directory
	 * @param fileName File name
	 * @param content File content
	 * @param queryKey Query keywords
	 * @param thinkActRecordId Think-act record ID for sub-plan execution tracking
	 * @param outputFormatSpecification A file used to describe in what format the data
	 * should be stored (default is an excel table), the table header of this file is the
	 * specification description
	 * @return Future of summarization result
	 */
	public CompletableFuture<String> executeSummaryWorkflow(String parentPlanId, String fileName, String content,
			String queryKey, Long thinkActRecordId, String outputFormatSpecification) {

		// 1. Build MapReduce execution plan using caller's planId
		MapReduceExecutionPlan executionPlan = buildSummaryExecutionPlan(parentPlanId, fileName, content, queryKey,
				outputFormatSpecification);

		// 2. Execute plan directly, passing thinkActRecordId
		return executeMapReducePlanWithContext(parentPlanId, executionPlan, thinkActRecordId);
	}

	/**
	 * Build MapReduce-based summarization execution plan
	 * @param parentPlanId Use caller-provided plan ID to ensure subprocess can find the
	 * corresponding directory
	 * @param fileName File name to be processed
	 * @param content File content (not directly used yet, but kept as extension
	 * parameter)
	 * @param queryKey Query keywords for information extraction
	 * @param outputFormatSpecification A file used to describe in what format the data
	 * should be stored (default is an excel table), the table header of this file is the
	 * specification description
	 * @return MapReduceExecutionPlan object configured based on the input parameters
	 */
	private MapReduceExecutionPlan buildSummaryExecutionPlan(String parentPlanId, String fileName, String content,
			String queryKey, String outputFormatSpecification) {

		try {
			// Use caller-provided planId instead of generating a new one
			logger.info("Building summary execution plan with provided planId: {}", parentPlanId);

			// Generate plan JSON using local template with PromptTemplate
			PromptTemplate promptTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.template(getSummaryPlanTemplate())
				.build();

			Map<String, Object> variables = new HashMap<>();
			variables.put("planId", parentPlanId);
			variables.put("fileName", fileName);
			variables.put("queryKey", queryKey);
			variables.put("outputFormatSpecification", outputFormatSpecification);

			String planJson = promptTemplate.render(variables);

			// Parse JSON to MapReduceExecutionPlan object
			MapReduceExecutionPlan plan = objectMapper.readValue(planJson, MapReduceExecutionPlan.class);
			// Output format specifications are configured directly in JSON template, no
			// need to set here

			return plan;

		}
		catch (Exception e) {
			logger.error("Failed to build summary execution plan, planId: {}", parentPlanId, e);
			throw new RuntimeException("Failed to build MapReduce summary execution plan: " + e.getMessage(), e);
		}
	}

	/**
	 * Execute MapReduce plan - supports sub-plan context
	 */
	private CompletableFuture<String> executeMapReducePlanWithContext(String rootPlanId,
			MapReduceExecutionPlan executionPlan, Long thinkActRecordId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// Generate a unique sub-plan ID using PlanIdDispatcher, similar to
				// generatePlan method

				String subPlanId = planIdDispatcher.generateSubPlanId(rootPlanId, thinkActRecordId);

				logger.info("Generated sub-plan ID: {} for parent plan: {}, think-act record: {}", subPlanId,
						rootPlanId, thinkActRecordId);

				// Get planning coordinator using generated sub-plan ID
				PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(subPlanId);

				// Create execution context
				ExecutionContext context = new ExecutionContext();
				context.setCurrentPlanId(subPlanId);
				context.setRootPlanId(rootPlanId);
				context.setThinkActRecordId(thinkActRecordId);

				// Update execution plan ID to sub-plan ID
				executionPlan.setCurrentPlanId(subPlanId);
				executionPlan.setRootPlanId(rootPlanId);
				context.setPlan(executionPlan);
				context.setNeedSummary(false);
				context.setUserRequest("Execute MapReduce-based intelligent content summarization");

				// Set think-act record ID to support sub-plan execution
				if (thinkActRecordId != null) {
					context.setThinkActRecordId(thinkActRecordId);
				}

				// Execute plan (skip plan creation step, execute directly)
				planningCoordinator.executeExistingPlan(context);

				logger.info("MapReduce summary plan executed successfully, sub-plan ID: {}, parent plan ID: {}",
						subPlanId, rootPlanId);

				List<ExecutionStep> allSteps = context.getPlan().getAllSteps();
				ExecutionStep lastStep = allSteps.get(allSteps.size() - 1);
				return "getContent executed successfully, execution result log: " + lastStep.getResult();
			}
			catch (Exception e) {
				logger.error("MapReduce summary plan execution failed", e);
				return "❌ MapReduce content summarization execution failed: " + e.getMessage();
			}
		});
	}

}
