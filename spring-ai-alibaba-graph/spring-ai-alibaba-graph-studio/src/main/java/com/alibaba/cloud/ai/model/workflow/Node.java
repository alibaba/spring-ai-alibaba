/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.service.runner.RunnableModel;

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

	public String getId() {
		return id;
	}

	public Node setId(String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public Node setType(String type) {
		this.type = type;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public Node setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getDesc() {
		return desc;
	}

	public Node setDesc(String desc) {
		this.desc = desc;
		return this;
	}

	public Float getWidth() {
		return width;
	}

	public Node setWidth(Float width) {
		this.width = width;
		return this;
	}

	public Float getHeight() {
		return height;
	}

	public Node setHeight(Float height) {
		this.height = height;
		return this;
	}

	public Coordinate getPosition() {
		return position;
	}

	public Node setPosition(Coordinate position) {
		this.position = position;
		return this;
	}

	public Coordinate getPositionAbsolute() {
		return positionAbsolute;
	}

	public Node setPositionAbsolute(Coordinate positionAbsolute) {
		this.positionAbsolute = positionAbsolute;
		return this;
	}

	public Boolean getSelected() {
		return selected;
	}

	public Node setSelected(Boolean selected) {
		this.selected = selected;
		return this;
	}

	public Integer getzIndex() {
		return zIndex;
	}

	public Node setzIndex(Integer zIndex) {
		this.zIndex = zIndex;
		return this;
	}

	public String getSourcePosition() {
		return sourcePosition;
	}

	public Node setSourcePosition(String sourcePosition) {
		this.sourcePosition = sourcePosition;
		return this;
	}

	public String getTargetPosition() {
		return targetPosition;
	}

	public Node setTargetPosition(String targetPosition) {
		this.targetPosition = targetPosition;
		return this;
	}

	public NodeData getData() {
		return data;
	}

	public Node setData(NodeData data) {
		this.data = data;
		return this;
	}

	@Override
	public String id() {
		return id;
	}

	public static class Coordinate {

		private Float x;

		private Float y;

		public Float getX() {
			return x;
		}

		public Coordinate setX(Float x) {
			this.x = x;
			return this;
		}

		public Float getY() {
			return y;
		}

		public Coordinate setY(Float y) {
			this.y = y;
			return this;
		}

	}

}
