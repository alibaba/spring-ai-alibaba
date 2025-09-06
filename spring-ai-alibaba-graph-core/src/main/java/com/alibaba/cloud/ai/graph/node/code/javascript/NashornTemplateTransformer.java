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

import com.alibaba.cloud.ai.graph.node.code.TemplateTransformer;
import com.alibaba.cloud.ai.graph.node.code.entity.RunnerAndPreload;
import java.util.List;

/**
 * Nashorn code template transformer Used to convert user code into executable Java
 * programs
 *
 * @author XenoAmess
 * @since 2025-08-30 10:00
 */
public class NashornTemplateTransformer extends TemplateTransformer {

	@Override
	public String getRunnerScript() {
		return "";
	}

	@Override
	public RunnerAndPreload transformCaller(String code, List<Object> inputs) throws Exception {
		return new RunnerAndPreload(code, "");
	}

}
