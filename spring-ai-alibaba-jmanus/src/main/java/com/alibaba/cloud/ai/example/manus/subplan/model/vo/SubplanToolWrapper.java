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
package com.alibaba.cloud.ai.example.manus.subplan.model.vo;

import com.alibaba.cloud.ai.example.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.runtime.vo.PlanExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper class that extends AbstractBaseTool for SubplanToolDef
 * 
 * This allows integration with the existing tool registry system
 */
public class SubplanToolWrapper extends AbstractBaseTool<Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(SubplanToolWrapper.class);
    
    private final SubplanToolDef subplanTool;
    
    @Autowired
    private PlanTemplateService planTemplateService;
    
    public SubplanToolWrapper(SubplanToolDef subplanTool, String currentPlanId, String rootPlanId) {
        this.subplanTool = subplanTool;
        this.currentPlanId = currentPlanId;
        this.rootPlanId = rootPlanId;
    }
    
    @Override
    public String getServiceGroup() {
        return subplanTool.getServiceGroup();
    }
    
    @Override
    public String getName() {
        return subplanTool.getToolName();
    }
    
    @Override
    public String getDescription() {
        return subplanTool.getToolDescription();
    }
    
    @Override
    public String getParameters() {
        // This will be handled by the service layer
        return "{}";
    }
    
    @Override
    public Class<Map<String, Object>> getInputType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return mapClass;
    }
    
    @Override
    public ToolExecuteResult run(Map<String, Object> input) {
        try {
            logger.info("Executing subplan tool: {} with template: {}", 
                       subplanTool.getToolName(), subplanTool.getPlanTemplateId());
            
            // Execute the subplan using PlanTemplateService
            CompletableFuture<PlanExecutionResult> future = planTemplateService.executePlanByTemplateId(
                subplanTool.getPlanTemplateId(),
                rootPlanId,
                currentPlanId,
                input
            );
            
            PlanExecutionResult result = future.get();
            
            if (result.isSuccess()) {
                String output = result.getEffectiveResult();
                if (output == null || output.trim().isEmpty()) {
                    output = "Subplan executed successfully but no output was generated";
                }
                logger.info("Subplan execution completed successfully: {}", output);
                return new ToolExecuteResult(output);
            } else {
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "Subplan execution failed";
                logger.error("Subplan execution failed: {}", errorMsg);
                return new ToolExecuteResult("Subplan execution failed: " + errorMsg);
            }
            
        }  catch (InterruptedException e) {
            String errorMsg = "Subplan execution was interrupted";
            logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            return new ToolExecuteResult(errorMsg);
        } catch (ExecutionException e) {
            String errorMsg = "Subplan execution failed with exception: " + e.getCause().getMessage();
            logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
            return new ToolExecuteResult(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during subplan execution: " + e.getMessage();
            logger.error("{} for tool: {}", errorMsg, subplanTool.getToolName(), e);
            return new ToolExecuteResult(errorMsg);
        }
    }
    
    @Override
    public void cleanup(String planId) {
        // Cleanup logic for the subplan tool
        logger.debug("Cleaning up subplan tool: {} for planId: {}", subplanTool.getToolName(), planId);
    }
    
    @Override
    public String getCurrentToolStateString() {
        return "Ready";
    }
    
    // Getter for the wrapped subplan tool
    public SubplanToolDef getSubplanTool() {
        return subplanTool;
    }
}
