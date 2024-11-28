package com.alibaba.cloud.ai.observation.handler;

import com.alibaba.cloud.ai.service.impl.ObservationDetailServiceImpl;
import com.alibaba.cloud.ai.service.impl.ObservationServiceImpl;
import io.micrometer.observation.Observation;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/26
 */
public class ContextHandlerFactory {

	private final Map<Class<? extends Observation.Context>, ContextHandler<?>> handlers = new HashMap<>();

	public ContextHandlerFactory(ObservationServiceImpl observationService,
			ObservationDetailServiceImpl modelObservationDetailService) {
		handlers.put(AdvisorObservationContext.class, new AdvisorObservationContextHandler());
		handlers.put(ChatModelObservationContext.class,
				new ChatModelObservationContextHandler(observationService, modelObservationDetailService));
		handlers.put(ChatClientObservationContext.class,
				new ChatClientObservationContextHandler(observationService, modelObservationDetailService));
	}

	@SuppressWarnings("unchecked")
	public <T extends Observation.Context> ContextHandler<T> getHandler(T context) {
		return (ContextHandler<T>) handlers.get(context.getClass());
	}

}