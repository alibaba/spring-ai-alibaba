package com.alibaba.cloud.ai.service.runner;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.RunEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Runner abstract the running action of a runnable object(could be an app, node, etc.)
 */
public interface Runner {

	boolean support(String type);

	RunEvent run(RunnableModel model, Map<String, Object> inputs);

	Flux<RunEvent> stream(RunnableModel model, Map<String, Object> inputs);

}
