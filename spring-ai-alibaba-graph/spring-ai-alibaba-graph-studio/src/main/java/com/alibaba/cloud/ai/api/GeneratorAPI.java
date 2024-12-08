package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.GenerateParam;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.generator.Generator;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.file.Path;

@Tag(name = "Generator", description = "API related to project generation")
public interface GeneratorAPI {

	Generator getGenerator(String appMode);

	DSLAdapter getDSLAdapter(String dialect);

	@RequestMapping(value = "/starter.zip", method = { RequestMethod.GET, RequestMethod.POST })
	default ResponseEntity<byte[]> generateZip(GenerateParam generateParam) {
		DSLAdapter dslAdapter = getDSLAdapter(generateParam.getDialect());
		App app = dslAdapter.importDSL(generateParam.getDsl());
		Generator generator = getGenerator(app.getMetadata().getMode());
		Path archive = generator.generate(app, generateParam);
		// TODO
		return null;
	}

}
