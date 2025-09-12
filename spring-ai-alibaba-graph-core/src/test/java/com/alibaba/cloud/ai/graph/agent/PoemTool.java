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
package com.alibaba.cloud.ai.graph.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.BiFunction;

public class PoemTool implements BiFunction<String, ToolContext, String> {

	public PoemTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The original user query that triggered this tool call") String originalUserQuery,
			ToolContext toolContext) {
		System.out.println("tool called : " + originalUserQuery);
		return "在城市的缝隙里，  \n" + "一束光悄悄发芽，  \n" + "穿过钢筋水泥的沉默，  \n" + "在风中轻轻说话。  \n" + "\n" + "夜色如墨，却不再黑，  \n"
				+ "星星点亮了每一个角落，  \n" + "我站在时间的边缘，  \n" + "等一朵云，轻轻落下";
	}

}
