package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeData;

import java.util.Map;

public interface WorkflowNodeDataConverter {

	Boolean supportType(String nodeType);

	WorkflowNodeData parseDifyData(Map<String, Object> data);

	Map<String, Object> dumpDifyData(WorkflowNodeData nodeData);

}
