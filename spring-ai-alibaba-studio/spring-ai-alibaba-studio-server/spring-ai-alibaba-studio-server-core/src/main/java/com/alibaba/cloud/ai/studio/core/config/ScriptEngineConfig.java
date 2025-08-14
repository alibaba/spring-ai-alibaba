package com.alibaba.cloud.ai.studio.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngineManager;

/**
 * Configuration class for script engines Registers and configures various script engines
 */
@Slf4j
@Configuration
public class ScriptEngineConfig {

	/**
	 * Registers and configures GraalVM Python engine
	 * @return configured ScriptEngineManager
	 */
	@Bean
	public ScriptEngineManager scriptEngineManager() {
		return new ScriptEngineManager();
	}

}
