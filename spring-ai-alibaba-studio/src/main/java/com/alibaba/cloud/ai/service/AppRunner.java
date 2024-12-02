package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.app.AppRunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * WorkflowDelegate defines the workflow execution operations.
 */
public interface AppRunner {

	AppRunEvent run(String id, Map<String, Object> inputs);

	Flux<AppRunEvent> stream(String id, Map<String, Object> inputs);
}
