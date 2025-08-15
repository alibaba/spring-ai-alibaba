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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Wrapper class that extends AbstractBaseTool for SubplanToolDef
 * 
 * This allows integration with the existing tool registry system
 */
public class SubplanToolWrapper extends AbstractBaseTool<Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(SubplanToolWrapper.class);
    
    private final SubplanToolDef subplanTool;
    
    public SubplanToolWrapper(SubplanToolDef subplanTool, String currentPlanId, String rootPlanId) {
        this.subplanTool = subplanTool;
        this.currentPlanId = currentPlanId;
        this.rootPlanId = rootPlanId;
    }
    
    @Override
    public String getServiceGroup() {
        return "subplan-tools";
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
        // This is where the subplan execution logic would be implemented
        // For now, return a placeholder result
        String inputInfo = input != null ? " with " + input.size() + " parameters" : "";
        return new ToolExecuteResult("Subplan tool '" + subplanTool.getToolName() + 
                                   "' executed" + inputInfo + ". Template: " + subplanTool.getPlanTemplateId());
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
