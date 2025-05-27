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

import java.util.List;

/**
 * @author yingzi
 * @date 2025/5/27 16:10
 */

public record ChatMessage(String role, List<ContentItem> conents) {
	public record ContentItem(
			/**
			 * 指明类型是text or image
			 */
			String type, String text, String imageUrl) {
	}
}
