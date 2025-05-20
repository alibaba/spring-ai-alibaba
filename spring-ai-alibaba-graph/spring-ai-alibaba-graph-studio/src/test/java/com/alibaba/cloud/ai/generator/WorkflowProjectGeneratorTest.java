package com.alibaba.cloud.ai.generator;

import com.alibaba.cloud.ai.app.AppDelegateTest;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import io.spring.initializr.generator.io.template.TemplateRenderer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorkflowProjectGeneratorTest {

	private static final Logger log = LoggerFactory.getLogger(AppDelegateTest.class);

	@Test
	void testRender() throws IOException {
		TemplateRenderer templateRenderer = new MustacheTemplateRenderer("classpath:/templates");
		Map<String, Object> model = new HashMap<>();
		model.put("packageName", "com.test");
		model.put("stateSection", "renderedStateSection");
		model.put("nodeSection", "renderedNodeSection");
		model.put("edgeSection", "renderedEdgeSection");
		String renderedFile = templateRenderer.render("graph/GraphBuilder.java", model);
		log.info(renderedFile);
	}

}
