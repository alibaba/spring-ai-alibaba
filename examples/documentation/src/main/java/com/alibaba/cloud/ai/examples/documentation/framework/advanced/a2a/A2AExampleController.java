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

package com.alibaba.cloud.ai.examples.documentation.framework.advanced.a2a;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 提供 HTTP 接口来调�?A2A 示例
 */
@RestController
@RequestMapping("/api/a2a")
public class A2AExampleController {

	private final A2AExample a2aExample;

	@Autowired
	public A2AExampleController(A2AExample a2aExample) {
		this.a2aExample = a2aExample;
	}

	/**
	 * 运行统一�?A2A 演示
	 *
	 * @return 执行结果
	 */
	@GetMapping("/demo")
	public Map<String, Object> runDemo() {
		Map<String, Object> response = new HashMap<>();
		try {
			a2aExample.runDemo();
			response.put("status", "success");
			response.put("message", "A2A 一体化演示执行完成");
		}
		catch (Exception e) {
			response.put("status", "error");
			response.put("message", "执行演示时出�? " + e.getMessage());
			response.put("error", e.getClass().getSimpleName());
		}
		return response;
	}
}
