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
package com.alibaba.cloud.ai.graph.node.code.python3;

import com.alibaba.cloud.ai.graph.node.code.TemplateTransformer;

/**
 * @author HeYQ
 * @since 2025-01-06 22:19
 */
public class Python3TemplateTransformer extends TemplateTransformer {

	@Override
	public String getRunnerScript() {
		return String.join("\n",
				new String[] { "# declare main function", CODE_PLACEHOLDER, "import json",
						"from base64 import b64decode",
						"inputs_obj = json.loads(b64decode('" + INPUTS_PLACEHOLDER + "').decode('utf-8'))",
						"output_obj = main(*inputs_obj)", "output_json = json.dumps(output_obj, indent=4)",
						"result = f'''<<RESULT>>{output_json}<<RESULT>>'''", "print(result)" });
	}

}
