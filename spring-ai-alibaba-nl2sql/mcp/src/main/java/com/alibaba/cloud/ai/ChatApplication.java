package com.alibaba.cloud.ai;

import com.alibaba.cloud.ai.service.McpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = { "com.alibaba.cloud.ai" })
@AutoConfiguration
public class ChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider chatBiTools(McpService mcpService) {
		return MethodToolCallbackProvider.builder().toolObjects(mcpService).build();
	}

}
