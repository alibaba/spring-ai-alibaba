package com.alibaba.cloud.ai.example.manus.recorder;

import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

/**
 * 计划执行记录器接口，定义了记录和检索计划执行详情的方法。
 */
public interface PlanExecutionRecorder {

	/**
	 * 记录一个计划执行实例，返回其唯一标识符
	 * @param stepRecord 计划执行记录
	 * @return 计划ID
	 */
	String recordPlanExecution(PlanExecutionRecord stepRecord);

	/**
	 * 记录智能体执行实例，关联到特定的计划
	 * @param planId 计划ID
	 * @param agentRecord 智能体执行记录
	 * @return 智能体执行ID
	 */
	Long recordAgentExecution(String planId, AgentExecutionRecord agentRecord);

	/**
	 * 记录思考-行动执行实例，关联到特定的智能体执行
	 * @param planId 计划ID
	 * @param agentExecutionId 智能体执行ID
	 * @param thinkActRecord 思考-行动记录
	 */
	void recordThinkActExecution(String planId, Long agentExecutionId, ThinkActRecord thinkActRecord);

	/**
	 * 标记计划执行完成
	 * @param planId 计划ID
	 * @param summary 执行总结
	 */
	void recordPlanCompletion(String planId, String summary);

	/**
	 * 获取计划执行记录
	 * @param planId 计划ID
	 * @return 计划执行记录
	 */
	PlanExecutionRecord getExecutionRecord(String planId);

	/**
	 * 将指定计划ID的执行记录保存到持久化存储 此方法会递归调用 PlanExecutionRecord、AgentExecutionRecord 和
	 * ThinkActRecord 的 save 方法
	 * @param planId 要保存的计划ID
	 * @return 如果找到并保存了记录则返回 true，否则返回 false
	 */
	boolean savePlanExecutionRecords(String planId);

	/**
	 * 将所有执行记录保存到持久化存储 此方法会遍历所有计划记录并调用它们的 save 方法
	 */
	void saveAllExecutionRecords();

	/**
	 * 获取指定计划的当前活动智能体执行记录
	 * @param planId 计划ID
	 * @return 当前活动的智能体执行记录，如果没有则返回null
	 */
	AgentExecutionRecord getCurrentAgentExecutionRecord(String planId);

}
