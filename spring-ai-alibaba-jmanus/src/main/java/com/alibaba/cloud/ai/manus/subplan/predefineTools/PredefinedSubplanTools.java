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
package com.alibaba.cloud.ai.manus.subplan.predefineTools;

import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanParamDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined subplan tools configuration
 *
 * Contains definitions for all built-in subplan tools that will be automatically
 * registered when the application starts
 */
public class PredefinedSubplanTools {

	/**
	 * Get all predefined subplan tool definitions
	 * @return List of predefined subplan tool definitions
	 */
	public static List<SubplanToolDef> getAllPredefinedTools() {
		List<SubplanToolDef> tools = new ArrayList<>();

		// Add content extraction tools
		tools.add(createExtractRelevantContentTool());
		tools.add(createGetFolderContentTool());

		return tools;
	}

	/**
	 * Create extract relevant content tool definition
	 * @return Extract relevant content tool definition
	 */
	public static SubplanToolDef createExtractRelevantContentTool() {
		SubplanToolDef tool = new SubplanToolDef();
		tool.setToolName("extract_relevant_content");
		tool.setToolDescription(
				"Extract relevant content from single file with intelligent analysis and structured output");
		tool.setPlanTemplateId("extract_relevant_content_template");
		tool.setEndpoint("/api/subplan/extract-content");
		tool.setServiceGroup("content-processing");

		// Define tool parameters
		List<SubplanParamDef> parameters = new ArrayList<>();

		// File name parameter
		SubplanParamDef fileNameParam = new SubplanParamDef("fileName", "String", "File name to be processed", true);

		// Query key parameter
		SubplanParamDef queryKeyParam = new SubplanParamDef("queryKey", "String",
				"Query keywords for information extraction", true);

		// Output format specification parameter
		SubplanParamDef outputFormatParam = new SubplanParamDef("outputFormatSpecification", "String",
				"Output format specification for data storage", true);

		// Add parameters to tool
		parameters.add(fileNameParam);
		parameters.add(queryKeyParam);
		parameters.add(outputFormatParam);

		tool.setInputSchema(parameters);
		return tool;
	}

	/**
	 * Create get folder content tool definition
	 * @return Get folder content tool definition
	 */
	public static SubplanToolDef createGetFolderContentTool() {
		SubplanToolDef tool = new SubplanToolDef();
		tool.setToolName("get_folder_content");
		tool.setToolDescription(
				"Get content from all files in specified folder with intelligent analysis and structured output");
		tool.setPlanTemplateId("get_folder_content_template");
		tool.setEndpoint("/api/subplan/folder-content");
		tool.setServiceGroup("content-processing");

		// Define tool parameters
		List<SubplanParamDef> parameters = new ArrayList<>();

		// Folder name parameter
		SubplanParamDef folderNameParam = new SubplanParamDef("folderName", "String", "Folder name or relative path",
				true);

		// Query key parameter
		SubplanParamDef queryKeyParam = new SubplanParamDef("queryKey", "String",
				"Query keywords for information extraction", true);

		// Output format specification parameter
		SubplanParamDef outputFormatParam = new SubplanParamDef("outputFormatSpecification", "String",
				"Output format specification for data storage", true);

		// Add parameters to tool
		parameters.add(folderNameParam);
		parameters.add(queryKeyParam);
		parameters.add(outputFormatParam);

		tool.setInputSchema(parameters);
		return tool;
	}

}
