/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

public class ReviewerTool implements BiFunction<String, ToolContext, String> {
	public int count = 0;

	public ReviewerTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The poem or article that needs to be reviewed.") String article,
			ToolContext toolContext) {
		count++;
		System.out.println("Reviewer tool called : " + article);
		return "晨光初透，薄雾如纱，轻轻覆在西湖的湖面上。断桥残影映水，柳丝拂波，露珠悬于草尖，欲坠未坠。远处山色空蒙，画舫轻移，划开一池碧琉璃。风过处，荷香暗送，落叶轻旋，似在低语岁月的秘密。原来人间至美，不过西湖一瞬的静谧与诗意�?;
	}

	public static ToolCallback createReviewerToolCallback() {
		return FunctionToolCallback.builder("reviewer", new ReviewerTool())
				.description("用来评论或修改诗、散文的工具")
				.inputType(String.class)
				.build();
	}

	public static ToolCallback createReviewerToolCallback(String name, ReviewerTool reviewerTool) {
		return FunctionToolCallback.builder(name, reviewerTool)
				.description("用来评论或修改诗、散文的工具")
				.inputType(String.class)
				.build();
	}

}
