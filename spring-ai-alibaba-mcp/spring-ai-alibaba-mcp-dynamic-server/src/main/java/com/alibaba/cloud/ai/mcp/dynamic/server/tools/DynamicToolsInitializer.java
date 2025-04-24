package com.alibaba.cloud.ai.mcp.dynamic.server.tools;

import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.common.utils.JsonUtils;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic tools initializer to handle tool initialization and avoid circular dependencies
 */
@Component
public class DynamicToolsInitializer {

	private static final Logger logger = LoggerFactory.getLogger(DynamicToolsInitializer.class);

	private static final String TOOLS_CONFIG_SUFFIX = "-mcp-tools.json";

	private final NamingService namingService;

	private final ConfigService configService;

	private final NacosMcpRegistryProperties nacosMcpRegistryProperties;

	public DynamicToolsInitializer(NamingService namingService, ConfigService configService,
			NacosMcpRegistryProperties nacosMcpRegistryProperties) {
		this.namingService = namingService;
		this.configService = configService;
		this.nacosMcpRegistryProperties = nacosMcpRegistryProperties;
	}

	public List<ToolCallback> initializeTools() {
		List<ToolCallback> allTools = new ArrayList<>();
		int pageNo = 1;
		int pageSize = 100;
		int totalCount = 0;

		try {
			do {
				ListView<String> services = namingService.getServicesOfServer(pageNo, pageSize,
						nacosMcpRegistryProperties.getServiceGroup());

				if (pageNo == 1) {
					totalCount = services.getCount();
					logger.info("Initial tools loading - Total count of services: {}", totalCount);
				}

				List<String> currentPageData = services.getData();
				logger.info("Initial tools loading - Page {} - Found {} services", pageNo, currentPageData.size());

				for (String serviceName : currentPageData) {
					try {
						String toolConfig = configService.getConfig(serviceName + TOOLS_CONFIG_SUFFIX,
								nacosMcpRegistryProperties.getServiceGroup(), 5000);

						if (toolConfig != null) {
							DynamicNacosToolsInfo toolsInfo = JsonUtils.deserialize(toolConfig,
									DynamicNacosToolsInfo.class);
							List<DynamicNacosToolDefinition> toolsInNacos = toolsInfo.getTools();

							if (!CollectionUtils.isEmpty(toolsInNacos)) {
								for (DynamicNacosToolDefinition toolDefinition : toolsInNacos) {
									toolDefinition.setServiceName(serviceName);
									allTools.add(new DynamicNacosToolCallback(toolDefinition));
								}
							}
						}
					}
					catch (Exception e) {
						logger.error("Failed to initialize tools for service: {}", serviceName, e);
					}
				}

				int startIndex = (pageNo - 1) * pageSize;
				if (startIndex + currentPageData.size() >= totalCount) {
					break;
				}
				pageNo++;
			}
			while (true);

			logger.info("Initial tools loading completed - Found {} tools", allTools.size());

		}
		catch (Exception e) {
			logger.error("Failed to initialize tools", e);
		}

		return allTools;
	}

}