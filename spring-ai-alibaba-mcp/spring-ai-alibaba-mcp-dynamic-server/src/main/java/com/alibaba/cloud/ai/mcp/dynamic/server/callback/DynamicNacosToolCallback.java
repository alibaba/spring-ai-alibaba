package com.alibaba.cloud.ai.mcp.dynamic.server.callback;

import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;

import java.util.concurrent.CompletableFuture;

public class DynamicNacosToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(DynamicNacosToolCallback.class);

	private final ToolDefinition toolDefinition;

	public DynamicNacosToolCallback(final ToolDefinition toolDefinition) {
		this.toolDefinition = toolDefinition;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public String call(@NonNull final String input) {
		return call(input, new ToolContext(Maps.newHashMap()));
	}

	@Override
	public String call(@NonNull final String input, final ToolContext toolContext) {
		return CompletableFuture.supplyAsync(() -> "Tool callback result").join();
	}

}