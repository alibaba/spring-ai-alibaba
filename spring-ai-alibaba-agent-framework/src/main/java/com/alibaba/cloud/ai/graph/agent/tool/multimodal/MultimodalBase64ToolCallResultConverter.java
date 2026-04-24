/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tool.multimodal;

/**
 * {@link MultimodalToolCallResultConverter} configured for base64 output.
 *
 * <p>Use with {@code @Tool(resultConverter = MultimodalBase64ToolCallResultConverter.class)}
 * when the consumer needs inline data (e.g., returnDirect to client). Note: base64
 * increases token usage when sent to the model.
 */
public class MultimodalBase64ToolCallResultConverter extends MultimodalToolCallResultConverter {

	public MultimodalBase64ToolCallResultConverter() {
		super(OutputFormat.base64);
	}
}
