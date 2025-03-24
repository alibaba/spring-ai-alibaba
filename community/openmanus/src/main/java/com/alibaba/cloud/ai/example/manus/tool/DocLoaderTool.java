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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class DocLoaderTool implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(DocLoaderTool.class);

	private static String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "file_type": {
			            "type": "string",
			            "description": "(required) File type, such as pdf, text, docx, xlsx, csv, etc.."
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required) Get the absolute path of the file from the user request."
			        }
			    },
			    "required": ["file_type","file_path"]
			}
			""";

	private static final String name = "doc_loader";

	private static final String description = """
			Get the content information of a local file at a specified path.
			Use this tool when you want to get some related information asked by the user.
			This tool accepts the file path and gets the related information content.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static ToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new DocLoaderTool())
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public DocLoaderTool() {
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("DocLoaderTool toolInput:" + toolInput);
		try {
			Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
			});
			String fileType = (String) toolInputMap.get("file_type");
			String filePath = (String) toolInputMap.get("file_path");
			TikaDocumentParser parser = new TikaDocumentParser();
			List<Document> documentList = parser.parse(new FileInputStream(filePath));
			List<String> documentContents = documentList.stream()
				.map(document -> document.getFormattedContent())
				.collect(Collectors.toList());

			String documentContentStr = String.join("\n", documentContents);
			if (StringUtils.isEmpty(documentContentStr)) {
				return new ToolExecuteResult("No Related information");
			}
			else {
				return new ToolExecuteResult("Related information: " + documentContentStr);
			}
		}
		catch (Throwable e) {
			return new ToolExecuteResult("Error get Related information: " + e.getMessage());
		}
	}

	@Override
	public ToolExecuteResult apply(String s) {
		return run(s);
	}

}
