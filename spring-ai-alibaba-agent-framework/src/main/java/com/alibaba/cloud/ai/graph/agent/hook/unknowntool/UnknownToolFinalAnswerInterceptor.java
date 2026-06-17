/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hook.unknowntool;

import com.alibaba.cloud.ai.graph.agent.hook.AbstractFinalAnswerInterceptor;

/**
 * Disables tool exposure for the special final-answer turn triggered by
 * {@link UnknownToolGuardHook}.
 */
public final class UnknownToolFinalAnswerInterceptor extends AbstractFinalAnswerInterceptor {

	@Override
	protected String finalAnswerInstructionMetadataKey() {
		return UnknownToolGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY;
	}

	@Override
	public String getName() {
		return "UnknownToolFinalAnswerInterceptor";
	}

}

