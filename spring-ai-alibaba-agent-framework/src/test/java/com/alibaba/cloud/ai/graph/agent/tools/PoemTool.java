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
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.util.ReflectionUtils;

/**
 * Poem tool using @Tool annotation with MethodToolCallback.
 * This tool can be used to generate poems based on user queries.
 */
public class PoemTool {
	
	public int count = 0;

	public PoemTool() {
	}

	/**
	 * Generate a poem based on the user's query.
	 * 
	 * @param originalUserQuery the original user query that triggered this tool call
	 * @param toolContext the tool context
	 * @return a generated poem
	 */
	@Tool(description = "用来写诗的工具，可以根据用户的查询生成诗歌内�?)
	public String writePoem(
			@ToolParam(description = "The original user query that triggered this tool call") String originalUserQuery,
			ToolContext toolContext) {
		count++;
		System.out.println("Poem tool called : " + originalUserQuery);
		try {
			Thread.sleep(10000);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return "在城市的缝隙里，  \n" + "一束光悄悄发芽�? \n" + "穿过钢筋水泥的沉默，  \n" + "在风中轻轻说话�? \n" + "\n" + "夜色如墨，却不再黑，  \n"
				+ "星星点亮了每一个角落，  \n" + "我站在时间的边缘�? \n" + "等一朵云，轻轻落�?;
	}

	/**
	 * Create a ToolCallback using MethodToolCallback from the @Tool annotated method.
	 * 
	 * @return ToolCallback instance
	 */
	public static ToolCallback createPoemToolCallback() {
		return createPoemToolCallback("poem", new PoemTool());
	}

	/**
	 * Create a ToolCallback using MethodToolCallback from the @Tool annotated method.
	 * 
	 * @param name the tool name
	 * @param poemTool the PoemTool instance
	 * @return ToolCallback instance
	 */
	public static ToolCallback createPoemToolCallback(String name, PoemTool poemTool) {
		// Find the writePoem method using reflection
		java.lang.reflect.Method method = ReflectionUtils.findMethod(PoemTool.class, 
				"writePoem", String.class, ToolContext.class);
		
		if (method == null) {
			throw new IllegalStateException("Could not find writePoem method in PoemTool class");
		}

		// Create ToolDefinition using ToolDefinitions.builder() with the method
		ToolDefinition toolDefinition = ToolDefinitions.builder(method)
				.name(name)
				.build();

		// Build MethodToolCallback
		return MethodToolCallback.builder()
				.toolDefinition(toolDefinition)
				.toolMethod(method)
				.toolObject(poemTool)
				.build();
	}
}

