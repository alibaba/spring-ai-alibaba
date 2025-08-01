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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.service.Nl2SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * NL2SQL接口预留
 *
 * @author vlsmb
 * @since 2025/7/27
 */
@RestController
@RequestMapping("/nl2sql")
public class Nl2SqlController {

	private static final Logger log = LoggerFactory.getLogger(Nl2SqlController.class);

	private final Nl2SqlService nl2SqlService;

	public Nl2SqlController(Nl2SqlService nl2SqlService) {
		this.nl2SqlService = nl2SqlService;
	}

	@GetMapping("/nl2sql")
	public String nl2sql(@RequestParam("query") String query) {
		try {
			return this.nl2SqlService.apply(query);
		}
		catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		}
		catch (Exception e) {
			log.error("nl2sql Exception: {}", e.getMessage(), e);
			return "Error: " + e.getMessage();
		}
	}

}
