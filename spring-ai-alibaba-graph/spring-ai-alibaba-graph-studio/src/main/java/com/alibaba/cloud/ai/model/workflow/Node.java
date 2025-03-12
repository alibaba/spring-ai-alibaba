/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
