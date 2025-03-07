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
