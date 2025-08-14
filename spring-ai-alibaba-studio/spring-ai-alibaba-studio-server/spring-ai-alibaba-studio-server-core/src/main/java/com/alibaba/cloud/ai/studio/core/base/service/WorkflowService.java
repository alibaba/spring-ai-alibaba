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
 * @since 1.0.0-beta
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
