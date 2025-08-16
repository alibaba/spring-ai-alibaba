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
package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskStopRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import reactor.core.publisher.Flux;

/**
 * Title workflow service.<br>
 * Description tenant service.<br>
 *
 * @since 1.0.0.3
 */
public interface WorkflowService {

	/**
	 * Synchronous call for workflow
	 * @param request agent call response
	 * @return chat response
	 */
	WorkflowResponse call(WorkflowRequest request);

	/**
	 * Synchronous streaming call for workflow
	 * @param requestFlux agent call request
	 * @return agent call response
	 */
	Flux<WorkflowResponse> streamCall(Flux<WorkflowRequest> requestFlux);

	TaskRunResponse asyncCall(WorkflowRequest request);

	Boolean stop(TaskStopRequest request);

}
