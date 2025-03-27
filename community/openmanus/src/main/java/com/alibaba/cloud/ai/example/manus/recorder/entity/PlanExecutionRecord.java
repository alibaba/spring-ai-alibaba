package com.alibaba.cloud.ai.example.manus.recorder.entity;

import com.alibaba.cloud.ai.example.manus.flow.PlanStepStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划执行记录类，用于跟踪和记录PlanningFlow执行过程的详细信息。
 * 
 * 数据结构分为四个主要部分：
 * 
 * 1. 基本信息 (Basic Info)
 *    - id: 记录的唯一标识
 *    - planId: 计划的唯一标识
 *    - title: 计划标题
 *    - startTime: 执行开始时间
 *    - endTime: 执行结束时间
 *    - userRequest: 用户的原始请求
 * 
 * 2. 计划结构 (Plan Structure)
 *    - steps: 计划步骤列表
 *    - stepStatuses: 步骤状态列表
 *    - stepNotes: 步骤备注列表
 *    - stepAgents: 与每个步骤关联的智能体
 * 
 * 3. 执行过程数据 (Execution Data)
 *    - currentStepIndex: 当前执行的步骤索引
 *    - agentExecutionRecords: 每个智能体执行的记录
 *    - executorKeys: 执行者键列表
 *    - resultState: 共享结果状态
 * 
 * 4. 执行结果 (Execution Result)
 *    - completed: 是否完成
 *    - progress: 执行进度（百分比）
 *    - summary: 执行总结
 *    - statusCounts: 各状态的统计数据
 * 
 * @see PlanningFlow
 * @see AgentExecutionRecord
 * @see JsonSerializable
 */
public class PlanExecutionRecord implements JsonSerializable {
    
    // 记录的唯一标识符
    private String id;
    
    // 计划的唯一标识符
    private String planId;
    
    // 计划标题
    private String title;
    
    // 用户的原始请求
    private String userRequest;
    
    // 执行开始的时间戳
    private LocalDateTime startTime;
    
    // 执行结束的时间戳
    private LocalDateTime endTime;
    
    // 计划的步骤列表
    private List<String> steps;
    
    // 步骤状态列表（NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED）
    private List<String> stepStatuses;
    
    // 步骤备注列表
    private List<String> stepNotes;
    
    // 与每个步骤关联的智能体名称
    private List<String> stepAgents;
    
    // 当前执行的步骤索引
    private Integer currentStepIndex;

    // 执行者键列表
    private List<String> executorKeys;
    
    // 共享结果状态
    private Map<String, Object> resultState;
    
    // 是否完成
    private boolean completed;
    
    // 执行进度（百分比）
    private double progress;
    
    // 执行总结
    private String summary;
    
    // 各状态的统计数据
    private Map<String, Integer> statusCounts;

    // List to maintain the sequence of agent executions
    private List<AgentExecutionRecord> agentExecutionSequence;

    /**
     * 默认构造函数
     */
    public PlanExecutionRecord() {
        this.steps = new ArrayList<>();
        this.stepStatuses = new ArrayList<>();
        this.stepNotes = new ArrayList<>();
        this.stepAgents = new ArrayList<>();
        this.executorKeys = new ArrayList<>();
        this.resultState = new HashMap<>();
        this.statusCounts = new HashMap<>();
        this.startTime = LocalDateTime.now();
        this.progress = 0.0;
        this.completed = false;
        this.agentExecutionSequence = new ArrayList<>();
    }
    
    /**
     * 带参数的构造函数
     * @param planId 计划ID
     * @param title 计划标题
     * @param userRequest 用户请求
     */
    public PlanExecutionRecord(String planId, String title, String userRequest) {
        this();
        this.planId = planId;
        this.title = title;
        this.userRequest = userRequest;
    }
    
    /**
     * 添加一个执行步骤
     * @param step 步骤描述
     * @param agentName 执行智能体名称
     */
    public void addStep(String step, String agentName) {
        this.steps.add(step);
        this.stepStatuses.add(PlanStepStatus.NOT_STARTED.getValue());
        this.stepNotes.add("");
        this.stepAgents.add(agentName);
        updateProgress();
    }
    
    /**
     * 更新步骤状态
     * @param stepIndex 步骤索引
     * @param status 新状态
     */
    public void updateStepStatus(int stepIndex, String status) {
        if (stepIndex >= 0 && stepIndex < stepStatuses.size()) {
            stepStatuses.set(stepIndex, status);
            updateProgress();
            updateStatusCounts();
        }
    }
    
    /**
     * 添加步骤备注
     * @param stepIndex 步骤索引
     * @param note 备注内容
     */
    public void addStepNote(int stepIndex, String note) {
        if (stepIndex >= 0 && stepIndex < stepNotes.size()) {
            String existingNote = stepNotes.get(stepIndex);
            if (existingNote == null || existingNote.isEmpty()) {
                stepNotes.set(stepIndex, note);
            } else {
                stepNotes.set(stepIndex, existingNote + "\n" + note);
            }
        }
    }
    
    /**
     * 添加智能体执行记录
     * @param agentName 智能体名称
     * @param record 执行记录
     */
    public void addAgentExecutionRecord(AgentExecutionRecord record) {
        this.agentExecutionSequence.add(record);
    }

    /**
     * 获取按执行顺序排列的智能体执行记录列表
     * @return 执行记录列表
     */
    public List<AgentExecutionRecord> getAgentExecutionSequence() {
        return agentExecutionSequence;
    }

    public void setAgentExecutionSequence(List<AgentExecutionRecord> agentExecutionSequence) {
        this.agentExecutionSequence = agentExecutionSequence;
    }

    /**
     * 设置共享结果状态的值
     * @param key 键
     * @param value 值
     */
    public void setResultState(String key, Object value) {
        this.resultState.put(key, value);
    }
    
    /**
     * 获取共享结果状态的值
     * @param key 键
     * @return 值
     */
    public Object getResultState(String key) {
        return this.resultState.get(key);
    }
    
    /**
     * 计算和更新进度
     */
    private void updateProgress() {
        int completed = 0;
        for (String status : stepStatuses) {
            if (PlanStepStatus.COMPLETED.getValue().equals(status)) {
                completed++;
            }
        }
        
        this.progress = steps.size() > 0 ? (completed / (double) steps.size()) * 100.0 : 0.0;
        this.completed = completed == steps.size() && !steps.isEmpty();
    }
    
    /**
     * 更新状态统计数据
     */
    private void updateStatusCounts() {
        for (String status : PlanStepStatus.getAllStatuses()) {
            statusCounts.put(status, 0);
        }
        
        for (String status : stepStatuses) {
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }
    }
    
    /**
     * 完成执行，设置结束时间
     */
    public void complete(String summary) {
        this.endTime = LocalDateTime.now();
        this.completed = true;
        this.summary = summary;
    }
    
    /**
     * 生成计划文本表示
     * @return 计划的文本表示
     */
    public String generatePlanText() {
        StringBuilder planText = new StringBuilder();
        planText.append("Plan: ").append(title).append(" (ID: ").append(planId).append(")\n");
        
        for (int i = 0; i < planText.length() - 1; i++) {
            planText.append("=");
        }
        planText.append("\n\n");
        
        int completed = statusCounts.getOrDefault(PlanStepStatus.COMPLETED.getValue(), 0);
        int total = steps.size();
        
        planText.append(String.format("Progress: %d/%d steps completed (%.1f%%)\n", completed, total, progress));
        planText.append(String.format("Status: %d completed, %d in progress, ", 
                statusCounts.getOrDefault(PlanStepStatus.COMPLETED.getValue(), 0),
                statusCounts.getOrDefault(PlanStepStatus.IN_PROGRESS.getValue(), 0)));
        planText.append(String.format("%d blocked, %d not started\n\n", 
                statusCounts.getOrDefault(PlanStepStatus.BLOCKED.getValue(), 0),
                statusCounts.getOrDefault(PlanStepStatus.NOT_STARTED.getValue(), 0)));
        planText.append("Steps:\n");
        
        Map<String, String> statusMarks = PlanStepStatus.getStatusMarks();
        
        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            String status = i < stepStatuses.size() ? stepStatuses.get(i) : PlanStepStatus.NOT_STARTED.getValue();
            String notes = i < stepNotes.size() ? stepNotes.get(i) : "";
            String agent = i < stepAgents.size() ? stepAgents.get(i) : "";
            String statusMark = statusMarks.getOrDefault(status, statusMarks.get(PlanStepStatus.NOT_STARTED.getValue()));
            
            planText.append(String.format("%d. %s %s", i, statusMark, step));
            if (!agent.isEmpty()) {
                planText.append(" [Agent: ").append(agent).append("]");
            }
            planText.append("\n");
            
            if (notes != null && !notes.isEmpty()) {
                planText.append("   Notes: ").append(notes).append("\n");
            }
        }
        
        if (summary != null && !summary.isEmpty()) {
            planText.append("\nSummary:\n").append(summary);
        }
        
        return planText.toString();
    }

    /**
     * 保存记录到持久化存储
     * 空实现，由具体的存储实现来覆盖
     * 同时会递归保存所有AgentExecutionRecord
     * 
     * @return 保存后的记录ID
     */
    public String save() {
        // Empty implementation - will be overridden by storage implementation
        // Save all AgentExecutionRecords
        if (agentExecutionSequence != null) {
            for (AgentExecutionRecord agentRecord : agentExecutionSequence) {
                agentRecord.save();
            }
        }
        return this.planId;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(String userRequest) {
        this.userRequest = userRequest;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
        updateProgress();
    }

    public List<String> getStepStatuses() {
        return stepStatuses;
    }

    public void setStepStatuses(List<String> stepStatuses) {
        this.stepStatuses = stepStatuses;
        updateProgress();
        updateStatusCounts();
    }

    public List<String> getStepNotes() {
        return stepNotes;
    }

    public void setStepNotes(List<String> stepNotes) {
        this.stepNotes = stepNotes;
    }

    public List<String> getStepAgents() {
        return stepAgents;
    }

    public void setStepAgents(List<String> stepAgents) {
        this.stepAgents = stepAgents;
    }

    public Integer getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(Integer currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }
    public List<String> getExecutorKeys() {
        return executorKeys;
    }

    public void setExecutorKeys(List<String> executorKeys) {
        this.executorKeys = executorKeys;
    }

    public Map<String, Object> getResultState() {
        return resultState;
    }

    public void setResultState(Map<String, Object> resultState) {
        this.resultState = resultState;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }

    @Override
    public String toString() {
        return "PlanExecutionRecord{" +
                "id='" + id + '\'' +
                ", planId='" + planId + '\'' +
                ", title='" + title + '\'' +
                ", currentStepIndex=" + currentStepIndex +
                ", steps=" + steps.size() +
                ", progress=" + progress +
                ", completed=" + completed +
                '}';
    }

    @Override
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        appendField(json, "id", id, true);
        appendField(json, "planId", planId, true);
        appendField(json, "title", title, true);
        appendField(json, "userRequest", userRequest, true);
        
        if (startTime != null) {
            appendField(json, "startTime", startTime.toString(), true);
        }
        if (endTime != null) {
            appendField(json, "endTime", endTime.toString(), true);
        }
        
        appendField(json, "currentStepIndex", currentStepIndex, false);
        appendField(json, "progress", progress, false);
        appendField(json, "completed", completed, false);
        appendField(json, "summary", summary, true);
        
        // Add steps array
        if (steps != null && !steps.isEmpty()) {
            json.append("\"steps\":[");
            for (int i = 0; i < steps.size(); i++) {
                json.append("\"").append(escapeJson(steps.get(i))).append("\"");
                if (i < steps.size() - 1) {
                    json.append(",");
                }
            }
            json.append("],");
        }
        
        // Add stepStatuses array
        if (stepStatuses != null && !stepStatuses.isEmpty()) {
            json.append("\"stepStatuses\":[");
            for (int i = 0; i < stepStatuses.size(); i++) {
                json.append("\"").append(escapeJson(stepStatuses.get(i))).append("\"");
                if (i < stepStatuses.size() - 1) {
                    json.append(",");
                }
            }
            json.append("],");
        }
        
        // Add stepAgents array
        if (stepAgents != null && !stepAgents.isEmpty()) {
            json.append("\"stepAgents\":[");
            for (int i = 0; i < stepAgents.size(); i++) {
                json.append("\"").append(escapeJson(stepAgents.get(i))).append("\"");
                if (i < stepAgents.size() - 1) {
                    json.append(",");
                }
            }
            json.append("],");
        }
        
        
        // Add agentExecutionSequence as a JSON array
        if (agentExecutionSequence != null && !agentExecutionSequence.isEmpty()) {
            json.append("\"agentExecutionSequence\":[");
            for (int i = 0; i < agentExecutionSequence.size(); i++) {
                json.append(agentExecutionSequence.get(i).toJson());
                if (i < agentExecutionSequence.size() - 1) {
                    json.append(",");
                }
            }
            json.append("],");
        }

        // Add statusCounts object
        if (statusCounts != null && !statusCounts.isEmpty()) {
            json.append("\"statusCounts\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                json.append(entry.getValue());
                first = false;
            }
            json.append("},");
        }
        
        // Remove trailing comma if present
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
}
