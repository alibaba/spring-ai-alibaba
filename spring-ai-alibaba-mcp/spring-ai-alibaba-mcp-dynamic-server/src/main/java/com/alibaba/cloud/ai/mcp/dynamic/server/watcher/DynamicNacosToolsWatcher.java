package com.alibaba.cloud.ai.mcp.dynamic.server.watcher;

import com.alibaba.cloud.ai.mcp.dynamic.server.definiation.DynamicNacosToolDefinition;
import com.alibaba.cloud.ai.mcp.dynamic.server.provider.DynamicMcpToolsProvider;
import com.alibaba.cloud.ai.mcp.dynamic.server.tools.DynamicNacosToolsInfo;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.common.utils.JsonUtils;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DynamicNacosToolsWatcher implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(DynamicNacosToolsWatcher.class);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private static final long POLLING_INTERVAL = 30L; // 轮询间隔，单位秒

	private static final String toolsConfigSuffix = "-mcp-tools.json";

	private final NamingService namingService;

	private final ConfigService configService;

	private final NacosMcpRegistryProperties nacosMcpRegistryProperties;

	private final DynamicMcpToolsProvider dynamicMcpToolsProvider;

	public DynamicNacosToolsWatcher(final NamingService namingService, final ConfigService configService,
			final NacosMcpRegistryProperties nacosMcpRegistryProperties,
			final DynamicMcpToolsProvider dynamicMcpToolsProvider) {
		this.namingService = namingService;
		this.configService = configService;
		this.nacosMcpRegistryProperties = nacosMcpRegistryProperties;
		this.dynamicMcpToolsProvider = dynamicMcpToolsProvider;
		// 启动定时任务
		this.startScheduledPolling();
	}

	private void startScheduledPolling() {
		scheduler.scheduleAtFixedRate(this::watch, 0, POLLING_INTERVAL, TimeUnit.SECONDS);
		logger.info("Started scheduled service polling with interval: {} seconds", POLLING_INTERVAL);
	}

	public void stop() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
		logger.info("Stopped scheduled service polling");
	}

	private void watch() {
		int pageNo = 1; // 页码
		int pageSize = 100; // 每页大小
		int totalCount = 0;

		try {
			do {
				// 获取服务列表，第一个参数是pageNo，第二个参数是pageSize，第三个参数是groupName
				ListView<String> services = namingService.getServicesOfServer(pageNo, pageSize,
						nacosMcpRegistryProperties.getServiceGroup());

				// 第一次循环时记录总数
				if (pageNo == 1) {
					totalCount = services.getCount();
					logger.info("Total count of services: {}", totalCount);
				}

				List<String> currentPageData = services.getData();
				logger.info("Page {} - Found {} services", pageNo, currentPageData.size());
				logger.info("Services list: {}", currentPageData);

				for (String serviceName : currentPageData) {
					try {
						String toolConfig = configService.getConfig(serviceName + toolsConfigSuffix,
								nacosMcpRegistryProperties.getServiceGroup(), 5000);
						if (toolConfig == null) {
							logger.warn("No tool config found for service: {}", serviceName);
							continue;
						}
						logger.info("Subscribing to service: {}", serviceName);

						DynamicNacosToolsInfo toolsInfo = JsonUtils.deserialize(toolConfig,
								DynamicNacosToolsInfo.class);
						List<DynamicNacosToolDefinition> toolsInNacos = toolsInfo.getTools();
						if (CollectionUtils.isEmpty(toolsInNacos)) {
							logger.warn("No tools found in Nacos for service: {}", serviceName);
							continue;
						}
						for (DynamicNacosToolDefinition toolDefinition : toolsInNacos) {
							try {
								dynamicMcpToolsProvider.removeTool(toolDefinition.name());
							}
							catch (Exception e) {
								logger.error("Failed to remove tool: {}", toolDefinition.name(), e);
							}
							toolDefinition.setServiceName(serviceName);
							dynamicMcpToolsProvider.addTool(toolDefinition);
						}

						namingService.subscribe(serviceName, nacosMcpRegistryProperties.getServiceGroup(), this);
					}
					catch (NacosException e) {
						logger.error("Failed to get tool config for service: {}", serviceName, e);
						continue;
					}
				}

				// 计算是否还有下一页
				int startIndex = (pageNo - 1) * pageSize;
				if (startIndex + currentPageData.size() >= totalCount) {
					// 已经获取所有数据
					break;
				}

				// 继续查询下一页
				pageNo++;
			}
			while (true);

		}
		catch (NacosException e) {
			logger.error("Failed to poll services list", e);
		}
		catch (Exception e) {
			logger.error("Unexpected error during service polling", e);
		}
	}

	@Override
	public void onEvent(Event event) {
		if (event instanceof NamingEvent namingEvent) {
			// 处理服务实例变更事件
			logger.info("Received service instance change event for service: {}", namingEvent.getServiceName());
			List<Instance> instances = namingEvent.getInstances();
			logger.info("Updated instances count: {}", instances.size());

			// 打印每个实例的详细信息
			instances.forEach(instance -> {
				logger.info("Instance: {}:{} (Healthy: {}, Enabled: {}, Metadata: {})", instance.getIp(),
						instance.getPort(), instance.isHealthy(), instance.isEnabled(),
						JacksonUtils.toJson(instance.getMetadata()));
			});
		}
	}

}
