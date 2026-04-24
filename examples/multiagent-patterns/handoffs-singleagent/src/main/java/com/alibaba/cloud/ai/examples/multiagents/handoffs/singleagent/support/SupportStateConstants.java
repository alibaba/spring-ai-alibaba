/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.support;

/**
 * State keys and step names for the customer support handoffs workflow.
 */
public final class SupportStateConstants {

	private SupportStateConstants() {
	}

	public static final String CURRENT_STEP = "current_step";
	public static final String WARRANTY_STATUS = "warranty_status";
	public static final String ISSUE_TYPE = "issue_type";

	public static final String STEP_WARRANTY_COLLECTOR = "warranty_collector";
	public static final String STEP_ISSUE_CLASSIFIER = "issue_classifier";
	public static final String STEP_RESOLUTION_SPECIALIST = "resolution_specialist";

	public static final String WARRANTY_IN_WARRANTY = "in_warranty";
	public static final String WARRANTY_OUT_OF_WARRANTY = "out_of_warranty";

	public static final String ISSUE_HARDWARE = "hardware";
	public static final String ISSUE_SOFTWARE = "software";
}
