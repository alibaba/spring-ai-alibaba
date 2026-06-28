/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.examples.dingtalk;

import java.util.Map;

import com.alibaba.cloud.ai.messagechannel.publisher.MessagePublisher;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoPushController {

	private final MessagePublisher publisher;

	public DemoPushController(MessagePublisher publisher) {
		this.publisher = publisher;
	}

	/** Browser-friendly: open http://localhost:8080/demo/push?text=hello in any tab. */
	@GetMapping("/demo/push")
	public Map<String, String> pushViaQuery(@RequestParam(defaultValue = "Hello from agent") String text) {
		publisher.pushText("dingtalk", null, null, text);
		return Map.of("status", "sent", "text", text);
	}

	/** UTF-8 safe: send Chinese in JSON body, e.g. {"text":"机器人来报到了"}. */
	@PostMapping("/demo/push")
	public Map<String, String> pushViaBody(@RequestBody Map<String, String> body) {
		String text = body.getOrDefault("text", "Hello from agent");
		publisher.pushText("dingtalk", null, null, text);
		return Map.of("status", "sent", "text", text);
	}

}
