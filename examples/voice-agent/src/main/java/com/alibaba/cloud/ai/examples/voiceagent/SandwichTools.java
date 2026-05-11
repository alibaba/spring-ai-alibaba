/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.voiceagent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Sandwich shop order tools for the voice agent.
 */
@Component
public class SandwichTools {

	@Tool(name = "add_to_order", description = "Add an item to the customer's sandwich order.")
	public String addToOrder(
			@ToolParam(description = "The item to add (e.g. turkey, lettuce, swiss)") String item,
			@ToolParam(description = "Quantity to add") int quantity) {
		return "Added " + quantity + " x " + item + " to the order.";
	}

	@Tool(name = "confirm_order", description = "Confirm the final order with the customer.")
	public String confirmOrder(
			@ToolParam(description = "Summary of the order to confirm") String orderSummary) {
		return "Order confirmed: " + orderSummary + ". Sending to kitchen.";
	}
}
