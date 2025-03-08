/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.GeneratorAPI;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.generator.CodeGenerator;
import com.alibaba.cloud.ai.service.generator.ProjectGenerator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("graph-studio/api/generate")
public class GeneratorController implements GeneratorAPI {

	private final List<ProjectGenerator> projectGenerators;

	private final List<CodeGenerator> codeGenerators;

	private final List<DSLAdapter> dslAdapters;

	public GeneratorController(List<ProjectGenerator> projectGenerators, List<CodeGenerator> codeGenerators,
			List<DSLAdapter> dslAdapters) {
		this.projectGenerators = projectGenerators;
		this.codeGenerators = codeGenerators;
		this.dslAdapters = dslAdapters;
	}

	@Override
	public ProjectGenerator getProjectGenerator(String appMode) {
		return projectGenerators.stream()
			.filter(generator -> generator.supportAppMode(appMode))
			.findFirst()
			.orElse(null);
	}

	@Override
	public CodeGenerator getCodeGenerator(String nodeType) {
		return codeGenerators.stream()
			.filter(generator -> generator.supportNodeType(nodeType))
			.findFirst()
			.orElse(null);
	}

	@Override
	public DSLAdapter getDSLAdapter(DSLDialectType dialectType) {
		return dslAdapters.stream().filter(adapter -> adapter.supportDialect(dialectType)).findFirst().orElse(null);
	}

}
