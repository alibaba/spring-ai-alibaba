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
package com.alibaba.cloud.ai.manus.subplan.templates;

import java.util.HashMap;
import java.util.Map;

/**
 * Predefined plan templates for subplan tools
 *
 * Contains all the plan templates that will be automatically created when the application
 * starts
 */
public class SubplanPlanTemplates {

	/**
	 * Get all predefined plan templates
	 * @return Map of template ID to template content
	 */
	public static Map<String, String> getAllPlanTemplates() {
		Map<String, String> templates = new HashMap<>();

		// Summary workflow template
		templates.put("summary_workflow_template", getSummaryWorkflowTemplate());

		// Content extraction templates
		templates.put("extract_relevant_content_template", getExtractRelevantContentTemplate());
		templates.put("get_folder_content_template", getGetFolderContentTemplate());

		return templates;
	}

	/**
	 * Get summary workflow plan template
	 * @return Summary workflow template JSON
	 */
	public static String getSummaryWorkflowTemplate() {
		return """
				{
				  "planType": "advanced",
				  "planId": "<<planId>>",
				  "title": "Intelligent content summarization for large files, with final merged file name output in summary",
				  "steps": [
				    {
				      "type": "mapreduce",
				      "dataPreparedSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_DATA_PREPARE_AGENT] Use map_reduce_tool to split content of file <<fileName>>"
				        }
				      ],
				      "mapSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_MAP_TASK_AGENT] Analyze file, find key information related to ```<<queryKey>>```, information should be comprehensive, including all data, facts and opinions, comprehensive information without omission. Output format specification: ``` <<outputFormatSpecification>>```. File format requirement: Markdown."
				        }
				      ],
				      "reduceSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_REDUCE_TASK_AGENT] Merge the information from this chunk into file, while maintaining information integrity, merge all content and remove results with no content found. Output format specification: <<outputFormatSpecification>>. File format requirement: Markdown."
				        }
				      ],
				      "postProcessSteps": [
				        {
				          "stepRequirement": "[MAPREDUCE_FIN_AGENT] After export completion, read the exported results and output all exported content completely. Output format specification: <<outputFormatSpecification>>. File format requirement: Markdown."
				        }
				      ]
				    }
				  ]
				}""";
	}

	/**
	 * Get extract relevant content plan template
	 * @return Extract relevant content template JSON
	 */
	public static String getExtractRelevantContentTemplate() {
		return """
				{
				  "planType": "advanced",
				  "planId": "<<planId>>",
				  "title": "Extract relevant content from single file with intelligent analysis",
				  "steps": [
				    {
				      "type": "sequential",
				      "steps": [
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Read and analyze the file <<fileName>> to understand its content structure"
				        },
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Extract information related to <<queryKey>> from the file content, ensuring comprehensive coverage of all relevant data, facts, and opinions"
				        },
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Format the extracted content according to <<outputFormatSpecification>> requirements and organize it systematically"
				        }
				      ]
				    }
				  ]
				}""";
	}

	/**
	 * Get get folder content plan template
	 * @return Get folder content template JSON
	 */
	public static String getGetFolderContentTemplate() {
		return """
				{
				  "planType": "advanced",
				  "planId": "<<planId>>",
				  "title": "Extract content from all files in specified folder with intelligent analysis",
				  "steps": [
				    {
				      "type": "sequential",
				      "steps": [
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Scan and analyze all files in folder <<folderName>> to understand their content structure"
				        },
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Extract information related to <<queryKey>> from all files, ensuring comprehensive coverage across the entire folder"
				        },
				        {
				          "stepRequirement": "[DEFAULT_AGENT] Consolidate and format the extracted content according to <<outputFormatSpecification>> requirements, organizing it systematically"
				        }
				      ]
				    }
				  ]
				}""";
	}

}
