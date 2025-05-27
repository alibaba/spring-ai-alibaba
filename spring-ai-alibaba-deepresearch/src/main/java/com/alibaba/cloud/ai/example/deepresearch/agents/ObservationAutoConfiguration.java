package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.ObservationProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Allen Hu
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties({ ObservationProperties.class })
@ConditionalOnProperty(prefix = ObservationProperties.PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class ObservationAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ObservationAutoConfiguration.class);

	@Bean
	public ObservationHandler<ToolCallingObservationContext> toolCallingObservationContextObservationHandler() {
		return new ObservationHandler<>() {
			@Override
			public boolean supportsContext(Observation.Context context) {
				return context instanceof ToolCallingObservationContext;
			}

			@Override
			public void onStart(ToolCallingObservationContext context) {
				ToolDefinition toolDefinition = context.getToolDefinition();
				logger.info("🔨ToolCalling start: {} - {}", toolDefinition.name(), context.getToolCallArguments());
			}

			@Override
			public void onStop(ToolCallingObservationContext context) {
				ToolDefinition toolDefinition = context.getToolDefinition();
				logger.info("✅ToolCalling done: {} - {}", toolDefinition.name(), context.getToolCallResult());
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean(name = "observationRegistry")
	public ObservationRegistry observationRegistry(
			ObjectProvider<ObservationHandler<?>> observationHandlerObjectProvider) {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		ObservationRegistry.ObservationConfig observationConfig = observationRegistry.observationConfig();
		observationHandlerObjectProvider.orderedStream().forEach(observationConfig::observationHandler);
		return observationRegistry;
	}

}
