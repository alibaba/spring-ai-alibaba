package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.List;
import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:52
 */
public interface AgentTypeProvider {
    // 类型标识，对齐 schema 中 agent.type，如 "ReactAgent"、"SequentialAgent"
    String type();

    // handle 的版本号（用于迁移）
    String handleVersion();

    // 返回该 type 的 handle 的 JSON Schema（前端表单渲染、校验）
    String jsonSchema();

    // 返回该 type 的 handle 默认值（前端新建时的初始值）
    Map<String, Object> defaultHandle();

    // 版本迁移（从旧版本 handle 升级到当前 handleVersion）
    Map<String, Object> migrate(Map<String, Object> oldHandle, String fromVersion);

    // 渲染代码分段：根据壳层 + handle + 子 Agent 变量名（父节点调用时传入）产出代码与导入
    CodeSections render(AgentShell shell, Map<String, Object> handle, RenderContext ctx, List<String> childVarNames);
}
