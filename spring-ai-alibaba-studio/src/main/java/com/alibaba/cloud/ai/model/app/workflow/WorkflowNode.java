package com.alibaba.cloud.ai.model.app.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowNode {

	private String id;

	private String type;

	private Map<String, Object> data;

	private Float width;

	private Float height;

	private Coordinate position;

	private Coordinate positionAbsolute;

	private Boolean selected;

	private Integer zIndex;

	private String sourcePosition;

	private String targetPosition;

}
