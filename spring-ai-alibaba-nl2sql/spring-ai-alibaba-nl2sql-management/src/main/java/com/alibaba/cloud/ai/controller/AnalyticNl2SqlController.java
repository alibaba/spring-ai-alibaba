package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.service.analytic.AnalyticNl2SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class AnalyticNl2SqlController {

	@Autowired
	private AnalyticNl2SqlService nl2SqlService;

	@PostMapping("/chat")
	public String nl2Sql(@RequestBody String input) throws Exception {
		return nl2SqlService.nl2sql(input);
	}

}
