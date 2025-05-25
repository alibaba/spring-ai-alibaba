package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.service.runner.RunnableModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Node defines the visual elements and behavior of a node in a workflow.
 */
@Data
@Accessors(chain = true)
public class Node implements RunnableModel {

	private String id;

	private String type;

	private String title;

	private String desc;

	private Float width;

	private Float height;

	private Coordinate position;

	private Coordinate positionAbsolute;

	private Boolean selected = false;

	private Integer zIndex = 0;

	private String sourcePosition;

	private String targetPosition;

	private NodeData data;

	@Override
	public String id() {
		return id;
	}

	@Data
	public static class Coordinate {

		private Float x;

		private Float y;

	}

}
