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
package com.alibaba.cloud.ai.graph.node.code.java;

import com.alibaba.cloud.ai.graph.node.code.TemplateTransformer;

/**
 * Java code template transformer Used to convert user code into executable Java programs
 *
 * @author HeYQ
 * @since 2024-06-01
 */
public class JavaTemplateTransformer extends TemplateTransformer {

	@Override
	public String getRunnerScript() {
		return """
				import java.util.*;
				import com.fasterxml.jackson.databind.ObjectMapper;
				import com.fasterxml.jackson.databind.node.ObjectNode;

				class Main {
				    public static void main(String[] args) throws Exception {
				        // Parse input parameters
				        String inputsBase64 = "%s";
				        String inputsJson = new String(Base64.getDecoder().decode(inputsBase64));
				        ObjectMapper mapper = new ObjectMapper();
				        Object[] inputs = mapper.readValue(inputsJson, Object[].class);

				        // Execute user code
				        Object result = main(inputs);

				        // Output results
				        String output = mapper.writeValueAsString(result);
				        System.out.println("<<RESULT>>" + output + "<<RESULT>>");
				    }

				    %s
				}""".formatted(INPUTS_PLACEHOLDER, CODE_PLACEHOLDER);
	}

}
