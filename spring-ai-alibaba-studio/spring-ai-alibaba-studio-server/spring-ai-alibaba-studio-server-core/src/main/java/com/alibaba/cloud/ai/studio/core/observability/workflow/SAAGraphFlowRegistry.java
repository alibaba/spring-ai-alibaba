package com.alibaba.cloud.ai.studio.core.observability.workflow;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SAAGraphFlowRegistry {

	private final ApplicationContext applicationContext;

	// flowId 为键
	private final Map<String, SAAGraphFlow> flowRegistry = new ConcurrentHashMap<>();

	public SAAGraphFlowRegistry(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	public void init() {
		Collection<SAAGraphFlow> flows = applicationContext.getBeansOfType(SAAGraphFlow.class).values();

		// 将它们注册到我们的 Map 中
		flows.forEach(flow -> {
			if (flowRegistry.containsKey(flow.graphId())) {
				throw new IllegalStateException("Duplicate Flow ID found: " + flow.graphId());
			}
			flowRegistry.put(flow.graphId(), flow);
		});

		System.out.println("Initialized GraphFlowRegistry. Found " + flowRegistry.size() + " flows.");
	}

	/**
	 * 根据 ownerID 查询并返回该用户拥有的所有流程。
	 * @param ownerID 用户的唯一标识符
	 * @return 该用户拥有的 SAAGraphFlow 列表
	 */
	public List<SAAGraphFlow> findByOwnerID(String ownerID) {
		if (ownerID == null || ownerID.isBlank()) {
			return List.of();
		}

		return flowRegistry.values()
			.stream()
			.filter(flow -> ownerID.equals(flow.ownerID()))
			.collect(Collectors.toList());
	}

	/**
	 * 根据 flowId 查询并返回指定的流程。
	 * @param flowId 流程的唯一标识符
	 * @return 匹配的 SAAGraphFlow，如果不存在则返回 null
	 */
	public SAAGraphFlow findById(String flowId) {
		if (flowId == null || flowId.isBlank()) {
			return null;
		}
		return flowRegistry.get(flowId);
	}

	/**
	 * 检查指定的 flowId 是否存在。
	 * @param flowId 流程的唯一标识符
	 * @return 如果存在返回 true，否则返回 false
	 */
	public boolean existsById(String flowId) {
		if (flowId == null || flowId.isBlank()) {
			return false;
		}
		return flowRegistry.containsKey(flowId);
	}

	/**
	 * 提供一个方法来获取所有流程
	 */
	public List<SAAGraphFlow> findAll() {
		return List.copyOf(flowRegistry.values());
	}

}
