package com.alibaba.cloud.ai.model.workflow.edge;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class WorkflowEdge {

	private String id;

	private String type;

	private String source;

	private String target;

	private List<Case> cases;

	// case id -> target id
	private Map<String, String> targetMap;

	private Map<String, Object> data;

	private Integer zIndex = 0;

	private Boolean selected = false;

}
