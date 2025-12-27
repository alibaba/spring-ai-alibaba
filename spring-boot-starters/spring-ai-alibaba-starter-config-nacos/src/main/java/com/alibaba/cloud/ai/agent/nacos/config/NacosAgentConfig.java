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

package com.alibaba.cloud.ai.agent.nacos.config;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.agent.nacos.NacosOptions;
import com.alibaba.cloud.ai.agent.nacos.ObservationConfiguration;
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
        String prefix = "spring.ai.alibaba.agent.proxy.nacos.";


        Set<String> propertyNames = environment.getPropertySources().stream()
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .flatMap(ps -> Arrays.stream(((EnumerablePropertySource<?>) ps).getPropertyNames()))
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toSet());


        propertyNames.forEach(name -> {
            String value = environment.getProperty(name);
            if (value != null) {
                props.setProperty(name.substring(prefix.length()), value);
            }
        });

        return props;
	}

	@Bean
	public NacosOptions nacosOptions(Properties nacosAgentProxyProperties, ObservationConfiguration observationConfiguration) throws NacosException {
		NacosOptions nacosOptions = new NacosOptions(nacosAgentProxyProperties);
		nacosOptions.setObservationConfiguration(observationConfiguration);
		if (nacosAgentProxyProperties.containsKey("promptKey")) {
			nacosOptions.setPromptKey(nacosAgentProxyProperties.getProperty("promptKey"));
		}

		if (nacosAgentProxyProperties.containsKey("agentName")) {
			nacosOptions.setAgentName(nacosAgentProxyProperties.getProperty("agentName"));
		}
		return nacosOptions;
	}

	@Bean
	public ObservationConfiguration observationConfiguration(ObjectProvider<ObservationRegistry> observationRegistry, ObjectProvider<ToolCallingManager> toolCallingManager,
			ObjectProvider<ChatModelObservationConvention> chatModelObservationConvention,
			ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention) {
		return new ObservationConfiguration(observationRegistry.getIfAvailable(), toolCallingManager.getIfAvailable(), chatModelObservationConvention.getIfAvailable(), chatClientObservationConvention.getIfAvailable());
	}

}
