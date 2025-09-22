package com.alibaba.cloud.ai.agent.nacos.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.agent.nacos.NacosOptions;
import com.alibaba.cloud.ai.agent.nacos.ObservationConfigration;
import com.alibaba.nacos.api.exception.NacosException;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "spring.ai.alibaba.agent.proxy.nacos.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class NacosAgentConfig {

	@Bean
	public Properties nacosAgentProxyProperties(ConfigurableEnvironment environment) {
		Properties props = new Properties();
		Map<String, Object> map = environment.getPropertySources().stream()
				.filter(ps -> ps instanceof EnumerablePropertySource)
				.map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
				.flatMap(Arrays::stream)
				.filter(name -> name.startsWith("spring.ai.alibaba.agent.proxy.nacos."))
				.collect(Collectors.toMap(
						key -> key.substring("spring.ai.alibaba.agent.proxy.nacos.".length()),
						environment::getProperty
				));

		map.forEach((k, v) -> props.setProperty(k, (String) v));
		return props;
	}

	@Bean
	public NacosOptions nacosOptions(Properties nacosAgentProxyProperties, ObservationConfigration observationConfigration) throws NacosException {
		NacosOptions nacosOptions = new NacosOptions(nacosAgentProxyProperties);
		nacosOptions.setObservationConfigration(observationConfigration);
		if (nacosAgentProxyProperties.containsKey("promptKey")) {
			nacosOptions.setPromptKey(nacosAgentProxyProperties.getProperty("promptKey"));
		}

		if (nacosAgentProxyProperties.containsKey("agentName")) {
			nacosOptions.setAgentName(nacosAgentProxyProperties.getProperty("agentName"));
		}
		return nacosOptions;
	}

	@Bean
	public ObservationConfigration observationConfigration(ObjectProvider<ObservationRegistry> observationRegistry, ObjectProvider<ToolCallingManager> toolCallingManager,
			ObjectProvider<ChatModelObservationConvention> chatModelObservationConvention,
			ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention) {
		return new ObservationConfigration(observationRegistry.getIfAvailable(), toolCallingManager.getIfAvailable(), chatModelObservationConvention.getIfAvailable(), chatClientObservationConvention.getIfAvailable());
	}

}
