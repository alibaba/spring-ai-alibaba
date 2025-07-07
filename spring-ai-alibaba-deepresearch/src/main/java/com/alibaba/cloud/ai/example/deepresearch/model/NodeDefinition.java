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

package com.alibaba.cloud.ai.example.deepresearch.model;

/**
 * Node Description information of the node
 *
 * @author ViliamSun
 * @since 1.0.0
 */
public record NodeDefinition(
		// The name of the node.
		String name,
		// The description of the node.
		String description) {

	public record SelectionNode(
			// The reasoning behind the selection.
			String reasoning,
			// The name of the selected node.
			String selection) {
		@Override
		public String toString() {
			return String.format("{\"reasoning\": \"%s\", \"selection\": \"%s\"}", reasoning, selection);
		}
	}
}
