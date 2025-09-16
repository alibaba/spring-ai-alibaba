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
package com.alibaba.cloud.ai.graph.node.code.javascript;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeStyle;
import com.alibaba.cloud.ai.graph.node.code.TemplateTransformer;

/**
 * @author HeYQ
 * @since 2025-01-06 23:03
 */
public class NodeJsTemplateTransformer extends TemplateTransformer {

	@Override
	public String getRunnerScript(CodeStyle style) {
		return switch (style) {
			case EXPLICIT_PARAMETERS -> String.format("""
					// declare main function
					%s

					// decode and prepare input object
					var inputs_obj = JSON.parse(Buffer.from('%s', 'base64').toString('utf-8'))

					// execute main function
					var output_obj = main(inputs_obj)

					// convert output to json and print
					var output_json = JSON.stringify(output_obj)
					var result = `%s${output_json}%s`
					console.log(result)
					""", CODE_PLACEHOLDER, INPUTS_PLACEHOLDER, RESULT_TAG, RESULT_TAG);
			case GLOBAL_DICTIONARY -> String.format("""
					// decode and prepare input object
					let params = JSON.parse(Buffer.from('%s', 'base64').toString('utf-8'))

					// declare main function
					%s

					// execute main function
					var output_obj = main()

					// convert output to json and print
					var output_json = JSON.stringify(output_obj)
					var result = `%s${output_json}%s`
					console.log(result)
					""", INPUTS_PLACEHOLDER, CODE_PLACEHOLDER, RESULT_TAG, RESULT_TAG);
		};
	}

}
