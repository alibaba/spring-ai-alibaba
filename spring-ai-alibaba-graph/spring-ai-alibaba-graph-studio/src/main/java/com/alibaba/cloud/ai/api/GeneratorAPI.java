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
package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.CodeGenerateParam;
import com.alibaba.cloud.ai.param.ProjectGenerateParam;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.generator.CodeGenerator;
import com.alibaba.cloud.ai.service.generator.ProjectGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.file.Path;

@Tag(name = "Generator", description = "API related to code/project generation")
public interface GeneratorAPI {

	ProjectGenerator getProjectGenerator(String appMode);

	CodeGenerator getCodeGenerator(String nodeType);

	DSLAdapter getDSLAdapter(DSLDialectType dialectType);

	@Operation(summary = "Generate project from DSL", tags = { "Generator" })
	@RequestMapping(value = "/project", method = { RequestMethod.GET, RequestMethod.POST })
	default ResponseEntity<byte[]> generateZip(ProjectGenerateParam projectGenerateParam) {
		DSLDialectType dialectType = DSLDialectType.fromValue(projectGenerateParam.getDialect())
			.orElseThrow(
					() -> new NotImplementedException("Unsupported DSL dialect" + projectGenerateParam.getDialect()));
		DSLAdapter dslAdapter = getDSLAdapter(dialectType);
		App app = dslAdapter.importDSL(projectGenerateParam.getDsl());
		ProjectGenerator projectGenerator = getProjectGenerator(app.getMetadata().getMode());
		Path archive = projectGenerator.generate(app, projectGenerateParam);
		// TODO
		return null;
	}

	@Operation(summary = "Generate code from node data", tags = { "Generator" })
	@RequestMapping(value = "/code", method = { RequestMethod.GET, RequestMethod.POST })
	default R<String> generateCode(CodeGenerateParam codeGenerateParam) {
		// TODO
		return R.success("");
	}

}
