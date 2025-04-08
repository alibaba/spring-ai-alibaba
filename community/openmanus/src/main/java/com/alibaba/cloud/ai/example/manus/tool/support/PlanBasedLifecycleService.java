package com.alibaba.cloud.ai.example.manus.tool.support;

public interface PlanBasedLifecycleService {

	// /**
	// * 获取或创建指定 planId 的实例
	// *
	// * @param planId 计划ID
	// * @return 对应的实例
	// * @throws IllegalArgumentException 当 planId 为空时
	// */
	// T getInstance(String planId);

	/**
	 * 清理指定 planId 的所有相关资源
	 * @param planId 计划ID
	 */
	void cleanup(String planId);

}
