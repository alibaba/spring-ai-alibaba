package com.alibaba.cloud.ai.model.workflow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Edge defines the routes between node and node. There are two types of Edge: Direct and
 * Conditional
 */
@Data
@Accessors(chain = true)
public class Edge {

	private String id;

	private String source;

	private String target;

	private String sourceHandle;

	private String targetHandle;

	private Map<String, Object> data;

	private Integer zIndex = 0;

}
