package com.alibaba.cloud.ai.mcp.dynamic.server.callback;

import com.alibaba.cloud.ai.mcp.dynamic.server.definiation.DynamicNacosToolDefinition;
import com.alibaba.cloud.ai.mcp.dynamic.server.utils.SpringBeanUtils;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

public class DynamicNacosToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(DynamicNacosToolCallback.class);

	private final ToolDefinition toolDefinition;

	private final WebClient webClient;

	private final NamingService namingService;

	public DynamicNacosToolCallback(final ToolDefinition toolDefinition) {
		this.toolDefinition = toolDefinition;
		this.webClient = SpringBeanUtils.getInstance().getBean(WebClient.class);
		this.namingService = SpringBeanUtils.getInstance().getBean(NamingService.class);
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
		try {
			logger.info("Tool callback input: {}, toolContext: {}", input, toolContext);
			DynamicNacosToolDefinition nacosToolDefinition = (DynamicNacosToolDefinition) this.toolDefinition;
			logger.info("Tool callback toolDefinition: {}", JacksonUtils.toJson(nacosToolDefinition));
			Instance instance = namingService.selectOneHealthyInstance(nacosToolDefinition.getServiceName());
			logger.info("Tool callback instance: {}", JacksonUtils.toJson(instance));
			String url = "http://" + instance.getIp() + ":" + instance.getPort() + nacosToolDefinition.getRequestPath();
			logger.info("Tool callback url: {}", url);
			if (nacosToolDefinition.getRequestMethod().equalsIgnoreCase("POST")) {
				return webClient.post().uri(url).bodyValue(input).retrieve().bodyToMono(String.class).block();
			}
			return webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}