# feature-15 DefaultBuilder resolver PR 修复（不含测试）

## 目标
- 修复 PR review 中的非测试问题：
  1) 支持多个 SkillsAgentHook 的 resolver 链式组合
  2) ToolCallbackResolver 只在 build 流程解析一次
  3) read_skill 工具名常量统一为 ReadSkillTool.READ_SKILL

## 验收标准
1. DefaultBuilder 不会在同一次 build 中重复解析 resolver
2. 多个 SkillsAgentHook 都会参与 resolver 链
3. SkillToolCallbackResolver 使用 ReadSkillTool.READ_SKILL 作为工具名

## 前端（可运行思路/片段）
- 不涉及前端改动

## Mock
- 不涉及

## 后端（实现/方案）
- DefaultBuilder：
  - 在 build() 里只解析一次 ToolCallbackResolver，并复用 this.resolver
  - gatherLocalTools() 不再重复解析 resolver
  - 取消 SkillsAgentHook 链式处理中的 break，允许多个 hook 参与
- SkillToolCallbackResolver：
  - READ_SKILL_TOOL_NAME 直接引用 ReadSkillTool.READ_SKILL

## 验收与测试
- 未新增测试（按需求排除）
- 建议后续补充：resolver 链式组合与 read_skill 解析的单测

## 改动历史
| Change ID | DateTime | Summary | Files | Verification | Notes |
|-----------|----------|---------|-------|--------------|-------|
| CHG-20260213-001 | 2026-02-13 10:40 | 修复 DefaultBuilder resolver 链与重复解析问题，并统一 read_skill 常量 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillToolCallbackResolver.java; docs/feature/feature-15-defaultbuilder-resolver-pr-fixes.md | 未测 | 不含测试变更 |
