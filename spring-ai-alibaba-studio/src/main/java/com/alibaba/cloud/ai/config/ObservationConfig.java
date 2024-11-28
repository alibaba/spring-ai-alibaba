package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.observation.AlibabaObservationHandler;
import com.alibaba.cloud.ai.observation.handler.ContextHandlerFactory;
import com.alibaba.cloud.ai.service.impl.ObservationDetailServiceImpl;
import com.alibaba.cloud.ai.service.impl.ObservationServiceImpl;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationConfig {

	@Bean
	public ObservationRegistry observationRegistry(ContextHandlerFactory contextHandlerFactory) {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig()
			.observationHandler(new AlibabaObservationHandler(contextHandlerFactory));
		return observationRegistry;
	}

	@Bean
	public ContextHandlerFactory contextHandlerFactory(ObservationServiceImpl observationService,
			ObservationDetailServiceImpl modelObservationDetailService) {
		return new ContextHandlerFactory(observationService, modelObservationDetailService);
	}

}
