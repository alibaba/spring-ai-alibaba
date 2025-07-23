/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.workflow;

import java.util.concurrent.CompletableFuture;

/**
 * Summary workflow interface, providing summary workflow functionality
 */
public interface ISummaryWorkflow {

	/**
	 * Execute summary workflow
	 * @param parentPlanId parent plan ID
	 * @param fileName file name
	 * @param content content
	 * @param queryKey query key
	 * @param thinkActRecordId think-act record ID
	 * @param terminateColumnsString terminate columns string
	 * @return asynchronous summary result
	 */
	CompletableFuture<String> executeSummaryWorkflow(String parentPlanId, String fileName, String content,
			String queryKey, Long thinkActRecordId, String terminateColumnsString);

}
