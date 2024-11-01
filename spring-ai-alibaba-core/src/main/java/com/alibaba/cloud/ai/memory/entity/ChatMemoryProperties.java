package com.alibaba.cloud.ai.memory.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Title memory-related configuration.<br>
 * Description Load configuration related to memory and model .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

	private String apiKey;

	private String modelName;

	private String memoryType;

	private String storageType;

	private float temperature;

	private int windowSize;

	private boolean includeSystemPrompt;

	private String prompt;

	private String transitUrl;

}
