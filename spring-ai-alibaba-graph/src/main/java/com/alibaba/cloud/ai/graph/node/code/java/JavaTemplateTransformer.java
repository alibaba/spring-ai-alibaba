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

import com.alibaba.cloud.ai.graph.node.code.entity.CodeStyle;
import com.alibaba.cloud.ai.graph.node.code.TemplateTransformer;

/**
 * Java code template transformer Used to convert user code into executable Java programs
 * user method should be {@code public static Object run([..args])}
 *
 * @author HeYQ
 * @since 2024-06-01
 */
public class JavaTemplateTransformer extends TemplateTransformer {

	@Override
	public String getRunnerScript(CodeStyle style) {
		return switch (style) {
			case EXPLICIT_PARAMETERS -> String.format(
					"""
							import java.util.*;
							import com.fasterxml.jackson.databind.ObjectMapper;
							import com.fasterxml.jackson.databind.node.ObjectNode;
							import com.fasterxml.jackson.core.type.TypeReference;
							import java.lang.reflect.Method;

							class Main {
							    public static void main(String[] args) throws Exception {
							        // Parse input parameters
							        String inputsBase64 = "%s";
							        String inputsJson = new String(Base64.getDecoder().decode(inputsBase64));
							        ObjectMapper mapper = new ObjectMapper();
							        Map<String, Object> inputs = mapper.readValue(inputsJson, new TypeReference<>(){});

							        // Execute user code
							        Object result = invokeMethod(Main.class, "run", inputs);

							        // Output results
							        String output = mapper.writeValueAsString(result);
							        System.out.println("%s" + output + "%s");
							    }

							    public static Object invokeMethod(Class<?> clazz, String methodName, Map<String, Object> args)
							        throws Exception {
							        Method func = Arrays.stream(clazz.getMethods()).filter(method -> method.getName().equals(methodName))
							                .filter(method -> method.getParameterCount() == args.size())
							                .findFirst()
							                .orElseThrow();
							        Object[] params = Arrays.stream(func.getParameters()).map(
							                parameter -> args.get(parameter.getName())
							        ).toArray();
							        return func.invoke(null, params);
							    }

							    // user code
							    %s
							}
							""",
					INPUTS_PLACEHOLDER, RESULT_TAG, RESULT_TAG, CODE_PLACEHOLDER);
			case GLOBAL_DICTIONARY -> String.format("""
					import java.util.*;
					import com.fasterxml.jackson.databind.ObjectMapper;
					import com.fasterxml.jackson.databind.node.ObjectNode;
					import com.fasterxml.jackson.core.type.TypeReference;

					class Main {

					    private static final Map<String, Object> params;

					    private static final ObjectMapper mapper;

					    static {
					        try {
					            // Parse input parameters
					            String inputsBase64 = "%s";
					            String inputsJson = new String(Base64.getDecoder().decode(inputsBase64));
					            mapper = new ObjectMapper();
					            params = mapper.readValue(inputsJson, new TypeReference<>(){});
					        } catch (Exception e) {
					            throw new RuntimeException(e);
					        }
					    }

					    public static void main(String[] args) throws Exception {
					        // Execute user code
					        Object result = run();

					        // Output results
					        String output = mapper.writeValueAsString(result);
					        System.out.println("%s" + output + "%s");
					    }

					    // user code
					    %s
					}
					""", INPUTS_PLACEHOLDER, RESULT_TAG, RESULT_TAG, CODE_PLACEHOLDER);
		};
	}

}
