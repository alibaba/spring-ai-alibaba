# 改动历史

| Change ID | DateTime | Type | Related Task IDs | Scope | Summary | Files | Risk/Impact | Verification | Next |
|-----------|----------|------|------------------|-------|---------|-------|-------------|--------------|------|
| CHG-20260131-001 | 2026-01-31 22:45 | docs | 1 | issue-3110 | 创建 Issue #3110 技术方案文档 - ToolContext metadata helper | docs/issue/3110-toolcontext-metadata-solution.md | 低（仅文档） | 文档结构完整 | 等待评审方案 |
| CHG-20260131-002 | 2026-01-31 23:00 | docs | 1 | issue-3110 | 更新方案：支持 Helper + Key 双模式并存，不废弃常量 | docs/issue/3110-toolcontext-metadata-solution.md | 低 | 已更新双模式设计 | 等待评审方案 |
| CHG-20260131-003 | 2026-01-31 23:15 | docs | 1 | issue-3110 | 添加完整测试案例（双模式验证），含可运行代码 | docs/issue/3110-toolcontext-metadata-solution.md | 低 | 测试案例覆盖两种访问方式 | 等待评审方案 |
| CHG-20260201-001 | 2026-02-01 00:30 | impl | 1 | issue-3110 | 实施 Phase 1-2: 创建 ToolContextHelper + 更新常量文档 + 示例代码 | spring-ai-alibaba-agent-framework/src/main/java/.../ToolContextHelper.java; ToolContextConstants.java; ToolContextDualAccessExample.java | 中 | 代码实现完成，待项目整体编译通过 | 等待项目整体构建修复 |
| CHG-20260205-001 | 2026-02-05 22:50 | backend | 2 | issue-4184 | 重写 Issue4184FullAgentTest：无需 LLM/API Key，稳定复现 HITL 审批后找不到 read_skill ToolCallback | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java; docs/feature.csv; docs/feature/feature-02-issue-4184-repro-case.md | 低（仅示例用例） | 本地运行 main，稳定复现异常 | 可进一步提取为 src/test 回归用例 |
| CHG-20260205-004 | 2026-02-05 23:40 | docs | 3 | issue-4184 | 输出 Issue #4184 修复多方案对比与推荐 | docs/feature/feature-04-issue-4184-fix-options.md; docs/feature.csv | 低（文档） | 未测 | 待方案评审 |
| CHG-20260207-001 | 2026-02-07 20:20 | docs | 4 | issue-4184 | 补充方案 B 改动要点说明 | docs/feature/feature-04-issue-4184-fix-options.md; docs/feature.csv; docs/_history.md | 低（文档） | 未测 | 待评审 |
| CHG-20260207-002 | 2026-02-07 20:20 | backend | 4 | issue-4184 | 实施 HITL hook 恢复时工具重注入 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/hip/HumanInTheLoopHook.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/ReactAgent.java; docs/feature/feature-05-issue-4184-hitl-hook-injection.md; docs/feature.csv; docs/_history.md | 中（恢复路径工具注入） | 未测 | 待评审 |
| CHG-20260207-003 | 2026-02-07 21:34 | backend | 5 | issue-4184 | 新增普通工具 HITL 丢失复现示例 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java; docs/feature/feature-06-normal-tool-hitl-loss-repro.md; docs/feature.csv | 低（示例用例） | 未测 | 可运行 main 验证 |
| CHG-20260207-004 | 2026-02-07 21:39 | backend | 6 | issue-4184 | 修复 NormalToolHITLLossTest 缺失 inputType 导致构建失败 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java; docs/feature/feature-06-normal-tool-hitl-loss-repro.md; docs/feature.csv; docs/_history.md | 低（示例用例） | 未测 | 运行 main 验证 |
| CHG-20260205-002 | 2026-02-05 23:18 | backend | 3 | issue-4184 | 修复 HITL 恢复后 skills 工具回调丢失问题 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/node/AgentToolNode.java; docs/feature.csv; docs/feature/feature-03-issue-4184-fix.md | 中（恢复逻辑影响工具执行路径） | 未测 | 建议运行 Issue4184FullAgentTest 验证 |
| CHG-20260205-003 | 2026-02-05 23:24 | backend | 3 | issue-4184 | 修复 SkillsAgentHook 缺少 Collectors 导致编译失败 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java; docs/_history.md; docs/feature/feature-03-issue-4184-fix.md | 低 | 本地 mvn install 通过 | 无 |
| CHG-20260207-005 | 2026-02-07 21:45 | test | 7 | issue-4184 | 调整 NormalToolHITLLossTest 保留工具回调，避免模拟丢失并更新文档 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java; docs/feature/feature-06-normal-tool-hitl-loss-repro.md; docs/feature.csv; docs/_history.md | 低（示例用例） | 未测 | 运行 main 验证 |
| CHG-20260207-006 | 2026-02-07 21:54 | test | 8 | hitl | 新增 HITL 工具保留验证示例 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/HITLToolRetentionCheck.java; docs/feature/feature-07-hitl-tool-retention-check.md; docs/feature.csv; docs/_history.md | 低（示例用例） | 未测 | 运行 main 验证 |
| CHG-20260207-007 | 2026-02-07 21:57 | test | 8 | hitl | 修复 HITLToolRetentionCheck 变量命名冲突 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/HITLToolRetentionCheck.java; docs/feature/feature-07-hitl-tool-retention-check.md; docs/_history.md | 低（示例用例） | 未测 | 运行 main 验证 |
| CHG-20260207-008 | 2026-02-07 23:01 | backend | 9 | issue-4184 | Persist skill activation state across model hooks and propagate to SkillsInterceptor metadata | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/interceptor/skills/SkillsInterceptor.java; docs/feature/feature-08-issue-4184-skills-context-retention.md; docs/feature.csv; docs/_history.md | 中（model hook metadata 更新） | 未测 | 建议运行 Issue4184FullAgentTest 验证 |
| CHG-20260207-009 | 2026-02-07 23:59 | backend | 10 | issue-4184 | HITL 恢复时仅对 skills 工具做补注入（方案B落地） | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/ReactAgent.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/hip/HumanInTheLoopHook.java; docs/feature/feature-04-issue-4184-fix-options.md; docs/feature.csv | 中（仅影响 HITL+skills 恢复路径） | 未测 | 建议跑 Issue4184FullAgentTest/HITLToolRetentionCheck |
| CHG-20260208-001 | 2026-02-08 00:33 | backend,docs,test | 11 | issue-4184 | 落地方案 A：ReactAgent 在 invoke/stream 前重注入 tools（补齐 resume 后丢失），并更新验证用例与方案文档 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/ReactAgent.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java; examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java; docs/feature/feature-04-issue-4184-fix-options.md; docs/feature.csv; docs/_history.md | 中（影响执行入口工具注入） | 本地运行 Issue4184FullAgentTest 通过 | 建议补充更多 resume/非 skills 回归用例 |
| CHG-20260208-002 | 2026-02-08 10:07 | backend,docs | 12 | sandbox-examples | 创建 4 个 Spring AI Alibaba Sandbox 示例模块并修复编译错误 | examples/sandbox-simple-tool/; examples/sandbox-structured-output/; examples/sandbox-custom/; examples/sandbox-browser-fullstack/; docs/_history.md | 低（新增示例） | 全部 4 个示例编译通过 | 建议补充集成测试 |
| CHG-20260208-003 | 2026-02-08 22:10 | verify | 12 | sandbox-examples | 验证所有 4 个 Sandbox 示例模块编译成功 | examples/sandbox-simple-tool/; examples/sandbox-structured-output/; examples/sandbox-custom/; examples/sandbox-browser-fullstack/backend/ | 无 | Maven clean compile 全部通过 | 任务完成 |

| CHG-20260208-004 | 2026-02-08 22:15 | process | 12 | sandbox-examples | 创建 examples/pom.xml 父项目配置，统一管理所有示例构建 | examples/pom.xml | 低（构建组织） | 父POM结构完整，支持快速检查profile | 补充Docker部署支持 |
| CHG-20260208-005 | 2026-02-08 22:15 | process | 12 | sandbox-examples | 创建 docker-compose.yml 支持一键启动所有示例服务 | examples/docker-compose.yml | 低（部署便利） | Docker Compose结构完整，包含安全代理 | 补充各示例Dockerfile |
| CHG-20260208-006 | 2026-02-08 22:20 | devops | 13 | sandbox-examples | 为所有4个Sandbox示例创建Dockerfile并更新docker-compose.yml | examples/sandbox-simple-tool/Dockerfile; examples/sandbox-structured-output/Dockerfile; examples/sandbox-custom/Dockerfile; examples/sandbox-browser-fullstack/backend/Dockerfile; examples/sandbox-browser-fullstack/frontend/Dockerfile; examples/sandbox-browser-fullstack/frontend/nginx.conf; examples/docker-compose.yml | 中（部署配置） | 所有Dockerfile使用多阶段构建、非root用户、健康检查；docker-compose.yml添加健康检查和依赖条件 | 待测试docker-compose up全栈启动 |
| CHG-20260208-007 | 2026-02-08 22:25 | devops | 13 | sandbox-examples | 创建GitHub Actions工作流实现Docker CI/CD自动化 | .github/workflows/docker-examples.yml; .hadolint.yaml | 低（CI配置） | 工作流包含: Hadolint Dockerfile检查、多镜像并行构建、Compose配置验证、Trivy安全扫描 | 等待GitHub Actions运行验证 |
| CHG-20260208-008 | 2026-02-08 22:30 | backend | 11 | issue-4184 | 创建 SkillToolCallbackResolver 用于从状态恢复技能工具 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillToolCallbackResolver.java | 中（新增核心组件） | 代码实现完成，遵循 ToolCallbackResolver 接口 | 需要与 AgentToolNode 集成验证 |
| CHG-20260208-009 | 2026-02-08 22:32 | backend | 11 | issue-4184 | 更新 SkillsAgentHook 在 beforeAgent 中持久化技能工具名称到状态 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java | 中（状态持久化） | SKILL_TOOLS_KEY 常量添加，工具名称持久化逻辑实现 | 需要验证状态写入正确性 |
| CHG-20260208-010 | 2026-02-08 22:35 | backend | 11 | issue-4184 | 更新 AgentToolNode 在 apply 中从状态恢复技能工具 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/node/AgentToolNode.java | 高（工具执行路径） | restoreSkillToolsFromState 方法添加，支持 checkpoint 恢复场景 | 需要运行 Issue4184FullAgentTest 验证 |
| CHG-20260208-011 | 2026-02-08 22:38 | test | 11 | issue-4184 | 更新 Issue4184FullAgentTest 验证完整的 Issue #4184 修复 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java | 低（测试验证） | 添加状态持久化验证，更新成功日志说明 | 运行测试验证完整修复流程 |
| CHG-20260208-013 | 2026-02-08 21:01 | backend | 14 | issue-4184 | DefaultBuilder 在存在 SkillsAgentHook 时自动链式配置 SkillToolCallbackResolver（统一用于 toolNames 解析与 AgentToolNode resolver） | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; docs/feature.csv; docs/_history.md | 中（构建器工具解析器装配） | 本地 `./mvnw -pl spring-ai-alibaba-agent-framework -DskipTests compile` 通过 | 建议补充 HITL resume 回归验证 |
| CHG-20260208-014 | 2026-02-08 21:10 | process,backend | 15 | issue-4184 | DefaultBuilder 复用原命名/结构（finalResolver）并补充注释，减少 review 噪音且保持行为不变 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; docs/feature.csv; docs/_history.md | 低（重构不改行为） | 本地 `./mvnw -pl spring-ai-alibaba-agent-framework -DskipTests compile` 通过 | 可继续验证 HITL resume 场景 |
| CHG-20260208-015 | 2026-02-08 21:22 | test,docs | 16 | issue-4184 | 修复 Issue4184FullAgentTest 常量引用：移除对已删除 SkillsAgentHook 常量的依赖，并校验 HITL 拦截与清空 toolCallbacks 后仍可按名称解析工具 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java; docs/feature.csv; docs/_history.md | 低（示例修正） | 本地 `./mvnw -f examples/studio-cors-conditional-it/pom.xml -DskipTests compile` 通过 | 可运行 main 进行端到端演示 |
| CHG-20260208-016 | 2026-02-08 21:25 | docs,test | 17 | issue-4184 | Issue4184FullAgentTest 默认关闭 agent verbose 日志，避免输出整段 Skills System；支持 -Dissue4184.verbose=true 开启 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java; docs/feature.csv; docs/_history.md | 低（仅输出控制） | 本地 `./mvnw -f examples/studio-cors-conditional-it/pom.xml -DskipTests compile` 通过 | 如需，可补充运行 main 的输出截图 |
| CHG-20260208-017 | 2026-02-08 21:42 | test,docs | 18 | issue-4184 | 强化 Issue4184FullAgentTest：运行时断言 read_skill 实际执行成功（按 call id + tool name 定位响应），并将 responseData 规范化后与 SkillRegistry.readSkillContent 对齐 | examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java; docs/feature.csv; docs/_history.md | 低（示例增强） | 本地 `./mvnw -f examples/studio-cors-conditional-it/pom.xml -DskipTests compile` 通过；`./mvnw -f examples/studio-cors-conditional-it/pom.xml -Dexec.mainClass=com.alibaba.cloud.ai.examples.studiocorsconditionalit.Issue4184FullAgentTest -Dexec.classpathScope=compile exec:java` 通过 | 可根据需要把该 main 提取为 JUnit 回归用例 |
| CHG-20260208-018 | 2026-02-08 22:55 | test,docs | 19 | agent-framework | 修复 CancellableAsyncToolCallbackTest 超时场景用例偶发失败（避免依赖 common pool 调度） | spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/tool/CancellableAsyncToolCallbackTest.java; docs/feature/feature-09-fix-agent-framework-test-flake.md; docs/feature.csv; docs/_history.md | 低（仅测试稳定性） | 本地 `./mvnw -pl spring-ai-alibaba-agent-framework test` 通过 | 无 |
| CHG-20260209-001 | 2026-02-09 10:30 | docs | 20 | sandbox-simple-tool | 新增 sandbox-simple-tool 使用/验证评估文档 | docs/feature/feature-10-sandbox-simple-tool-usage-review.md; docs/feature.csv; docs/_history.md | 低（文档） | 未测 | 待评审 |
| CHG-20260209-002 | 2026-02-09 22:27 | process,backend | 21 | merge-main | 合并本地 main 至 bugfix-lose-skills 并解决 DefaultBuilder 冲突（保留 skills resolver chain） | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; docs/feature/feature-11-merge-main-into-branch.md; docs/feature.csv; docs/_history.md | 中（合并变更） | 未测 | 建议跑相关模块编译/用例 |
| CHG-20260209-003 | 2026-02-09 23:10 | backend,process | 22 | issue-4184 | DefaultBuilder 对齐 main 的格式与命名，减少无关 diff | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; docs/feature/feature-12-defaultbuilder-format-sync.md; docs/feature.csv; docs/_history.md | 低（格式/命名调整） | 未测 | 建议跑相关模块编译 |
| CHG-20260209-004 | 2026-02-09 23:20 | backend,process | 23 | issue-4184 | 恢复 DefaultBuilder 中 this.resolver 的显式使用，保持 resolver chain 行为 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; docs/feature/feature-12-defaultbuilder-format-sync.md; docs/feature.csv; docs/_history.md | 低（命名/引用调整） | 未测 | 建议跑相关模块编译 |
| CHG-20260209-005 | 2026-02-09 23:40 | process | 24 | examples/studio-cors-conditional-it | 将示例项目依赖版本对齐为 1.1.3.0-SNAPSHOT 以使用本地 install 产物 | examples/studio-cors-conditional-it/pom.xml; docs/feature/feature-13-studio-cors-it-use-local-snapshot-deps.md; docs/feature.csv; docs/_history.md | 低（依赖版本调整） | 未测 | 建议重新刷新 Maven 依赖 |
| CHG-20260209-006 | 2026-02-09 23:55 | process | 25 | examples/studio-cors-conditional-it | 将示例项目 sandbox 依赖替换为 spring-ai-alibaba-sandbox 以解决 1.1.3.0-SNAPSHOT 解析失败 | examples/studio-cors-conditional-it/pom.xml; docs/feature/feature-14-studio-cors-it-sandbox-dep-fix.md; docs/feature.csv; docs/_history.md | 低（依赖坐标调整） | 未测 | 安装 sandbox 模块后重试构建 |
| CHG-20260210-001 | 2026-02-10 00:10 | verify | 25 | examples/studio-cors-conditional-it | 验证示例项目构建可解析 sandbox 依赖 | examples/studio-cors-conditional-it/pom.xml; docs/feature/feature-14-studio-cors-it-sandbox-dep-fix.md; docs/_history.md | 低（验证） | `./mvnw -f examples/studio-cors-conditional-it/pom.xml -DskipTests test` | 依赖解析通过 |
| CHG-20260210-002 | 2026-02-10 00:10 | verify | 24 | examples/studio-cors-conditional-it | 验证示例项目可解析本地 SNAPSHOT 依赖 | examples/studio-cors-conditional-it/pom.xml; docs/feature/feature-13-studio-cors-it-use-local-snapshot-deps.md; docs/_history.md | 低（验证） | `./mvnw -f examples/studio-cors-conditional-it/pom.xml -DskipTests test` | 依赖解析通过 |
| CHG-20260213-001 | 2026-02-13 10:40 | backend,process | 26 | issue-4184 | 修复 DefaultBuilder resolver 链与重复解析问题，并统一 read_skill 常量 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java; spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillToolCallbackResolver.java; docs/feature/feature-15-defaultbuilder-resolver-pr-fixes.md; docs/feature.csv; docs/_history.md | 低（构建器行为微调） | 未测 | 建议补充 resolver 链与 read_skill 解析单测 |

---

## DIA-20260209-005 | 2026-02-09 23:35 | Owner(你), Agent(Codex)

**Context**: 修改 DefaultBuilder 后，示例项目仍无法看到本地变更。

**Request**: 对齐示例项目依赖版本以使用本地 SNAPSHOT 产物。

**Decisions**:
1. 将 examples/studio-cors-conditional-it 依赖版本统一为 1.1.3.0-SNAPSHOT。
2. 通过本地 mvn install 输出至 ~/.m2，并刷新示例项目依赖。

**Open Questions**:
- 是否需要进一步清理本地旧 SNAPSHOT 缓存？

**References**:
- examples/studio-cors-conditional-it/pom.xml
- docs/feature/feature-13-studio-cors-it-use-local-snapshot-deps.md

## DIA-20260209-006 | 2026-02-09 23:50 | Owner(你), Agent(Codex)

**Context**: 示例项目在 1.1.3.0-SNAPSHOT 下解析 spring-ai-alibaba-sandbox-tool 失败。

**Request**: 修复示例项目的 sandbox 依赖坐标并完成构建验证。

**Decisions**:
1. 将依赖从 spring-ai-alibaba-sandbox-tool 替换为 spring-ai-alibaba-sandbox。
2. 安装 sandbox 模块后再执行示例项目构建验证。

**Open Questions**:
- 是否需要同步调整其他示例项目的 sandbox 依赖？

**References**:
- examples/studio-cors-conditional-it/pom.xml
- docs/feature/feature-14-studio-cors-it-sandbox-dep-fix.md

## DIA-20260209-003 | 2026-02-09 23:10 | Owner(你), Agent(Codex)

**Context**: DefaultBuilder 的合并改动显得杂乱，需要尽量贴近 main。

**Request**: 清理不必要的变量名/空行改动，保持与 main 风格一致且行为不变。

**Decisions**:
1. 统一使用 `finalResolver` 作为有效 resolver 命名。
2. imports 与空行分组对齐 main 风格，减少无关 diff。
3. 保持 skills resolver chain 行为不变。

**Open Questions**: 无

**References**:
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java
- docs/feature/feature-12-defaultbuilder-format-sync.md

## DIA-20260209-004 | 2026-02-09 23:20 | Owner(你), Agent(Codex)

**Context**: DefaultBuilder 中 `this.resolver` 的显式引用被替换，影响与 main 对齐感。

**Request**: 恢复 `this.resolver` 的可见使用，保持 skills resolver chain 行为不变。

**Decisions**:
1. build 与 gatherLocalTools 仍使用 `this.resolver` 作为主要引用。
2. 通过 resolveToolCallbackResolver 生成有效 resolver 并同步回 `this.resolver`。
3. 行为保持一致，减少 review 噪音。

**Open Questions**: 无

**References**:
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java
- docs/feature/feature-12-defaultbuilder-format-sync.md

## DIA-20260209-002 | 2026-02-09 22:27 | Owner(你), Agent(Codex)

**Context**: 需要将 main 合并到当前分支，且 DefaultBuilder 冲突较多。

**Request**: 直接在本地处理合并冲突并完成 merge。

**Decisions**:
1. 使用本地 `main` 进行合并（因 `git fetch origin` SSL 失败）。
2. 以 main 的结构化重构为主线，保留 skills resolver chain 行为。
3. 完成 merge commit 并记录到历史。

**Open Questions**:
- 是否需要补跑模块编译或相关用例验证本次合并？

**References**:
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java
- docs/feature/feature-11-merge-main-into-branch.md
- docs/feature.csv

## DIA-20260209-001 | 2026-02-09 10:30 | Owner(你), Agent(Codex)

**Context**: 需要判断 `examples/sandbox-simple-tool` 如何使用、如何验证，并评估是否真的使用到了 Sandbox。

**Request**: 产出可执行的使用/验证步骤，并给出“是否真实使用 Sandbox”的结论与依据。

**Decisions**:
1. 给出启动与调用步骤，明确 Docker 与 API Key 前置要求。
2. 验证分为两类：容器级（docker ps/logs）与工具级（触发 Python/Shell/Browser 工具）。
3. 明确结论：代码已集成 Sandbox，但是否执行取决于模型是否调用工具。

**Open Questions**:
- 是否需要新增强制触发 Sandbox 的测试用例（避免模型不调用工具导致验证不稳定）？

**References**:
- examples/sandbox-simple-tool/README.md
- examples/sandbox-simple-tool/src/main/java/com/alibaba/cloud/ai/examples/sandbox/simple/config/AgentConfiguration.java
- examples/sandbox-simple-tool/src/main/java/com/alibaba/cloud/ai/examples/sandbox/simple/config/SandboxConfiguration.java
- docs/feature/feature-10-sandbox-simple-tool-usage-review.md

## DIA-20260208-004 | 2026-02-08 22:20 | Owner(你), Agent(Codex)

**Context**: Task 13 需要为所有 Sandbox 示例创建 Dockerfile 以支持容器化部署。

**Request**: 继续完成 Task 13，为所有 4 个示例创建 Dockerfile 并完善 docker-compose.yml。

**Decisions**:
1. **Java 后端 Dockerfile 模式**（统一应用于 simple-tool, structured-output, custom, browser-backend）：
   - 多阶段构建（Maven 构建 + JRE 运行）
   - 使用 `eclipse-temurin:17-jre-alpine` 最小化镜像
   - 创建非 root 用户（UID 1000）运行应用
   - 添加健康检查（wget 检查 /actuator/health 或 /api/chat/health）
   - 利用层缓存优化（先复制 pom.xml 下载依赖）

2. **前端 Dockerfile 模式**（browser-fullstack/frontend）：
   - 多阶段构建（Node 构建 + Nginx 服务）
   - 使用 `node:20-alpine` 构建 React/Vite 应用
   - 使用 `nginx:alpine` 服务静态文件
   - 自定义 nginx.conf 配置反向代理到后端
   - 启用 gzip 压缩和静态资源缓存

3. **docker-compose.yml 优化**：
   - 将 docker-socket 服务移到最前面作为基础依赖
   - 添加健康检查配置到所有服务
   - 使用 `condition: service_healthy` 确保启动顺序
   - 移除直接挂载 docker.sock 的卷（改用 socket proxy）
   - 配置网络子网（172.20.0.0/16）

**Open Questions**:
- 是否需要为自定义 Sandbox 示例创建额外的 Sandbox 镜像？（当前使用agentscope-runtime-sandbox-core镜像）
- 是否需要添加 .dockerignore 文件优化构建上下文？

**Verification**:
- ✅ 所有 6 个 Dockerfile 已创建
- ✅ nginx.conf 已创建并配置反向代理
- ✅ docker-compose.yml 已更新健康检查和依赖关系
- ⏳ 待测试 `docker-compose up` 全栈启动

**References**:
- examples/sandbox-simple-tool/Dockerfile
- examples/sandbox-structured-output/Dockerfile
- examples/sandbox-custom/Dockerfile
- examples/sandbox-browser-fullstack/backend/Dockerfile
- examples/sandbox-browser-fullstack/frontend/Dockerfile
- examples/sandbox-browser-fullstack/frontend/nginx.conf
- examples/docker-compose.yml

---

## DIA-20260208-005 | 2026-02-08 22:55 | Owner(你), Agent(Codex)

**Context**: `spring-ai-alibaba-agent-framework` 模块单测出现失败，需要根据 surefire 报告定位并修复。

**Request**: 修复失败用例并确保模块测试稳定通过。

**Decisions**:
1. 将用例中长耗时任务从 `CompletableFuture.supplyAsync()`（默认 common pool）改为专用线程执行，避免线程池饥饿导致的随机超时。
2. 保持验证点不变：取消 token 后，工具应尽快结束并提前停止循环。

**Verification**:
- ✅ `./mvnw -pl spring-ai-alibaba-agent-framework test` 通过

**References**:
- spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/tool/CancellableAsyncToolCallbackTest.java
- docs/feature/feature-09-fix-agent-framework-test-flake.md

---

## DIA-20260208-003 | 2026-02-08 22:15 | Owner(你), Agent(Codex)

**Context**: 4个Sandbox示例已完成编译验证，需要进一步优化项目结构和部署便利性。

**Request**: 创建父POM统一管理示例构建，添加Docker Compose配置便于本地测试。

**Decisions**:
1. 创建`examples/pom.xml`作为父项目，包含：
   - 统一的版本管理（Spring AI Alibaba 1.1.2.0-SNAPSHOT）
   - 依赖管理BOM配置（spring-ai-bom, spring-ai-alibaba-bom等）
   - 插件配置（Maven Compiler + Lombok Annotation Processor）
   - 仓库配置（Spring milestones/snapshots, Aliyun）
   - Profiles支持：`fullstack`（包含browser-fullstack）、`quick-check`（仅编译检查）

2. 创建`examples/docker-compose.yml`支持：
   - 4个后端服务的容器化部署
   - browser-fullstack的前端nginx部署
   - Docker Socket Proxy安全代理
   - 统一网络配置（sandbox-network）

**Next**:
- 补充各示例的Dockerfile
- 考虑添加集成测试
- 更新README添加Docker部署说明

**References**:
- examples/pom.xml
- examples/docker-compose.yml
- examples/README.md

---


## DIA-20260131-001 | 2026-01-31 22:30 | Owner, Agent(Codex)

**Context**: 收到 Issue #3110 分析请求，关于 ToolContext 获取 metadata 方式与文档不一致的问题。

**Request**: 分析问题并设计技术方案，产出到 docs/issue/ 目录。

**Decisions**:
1. 问题根因：文档示例使用 `"config"`、`"state"` 等直观 key，但实际实现使用 `"_AGENT_CONFIG_"`、`"_AGENT_STATE_"` 等内部常量
2. **最终方案**：提供 `ToolContextHelper` 工具类 + 保留 `ToolContextConstants` 常量直接访问，**两种方式并存**
   - Helper 方式：类型安全、简洁，适合常用标准数据访问
   - 直接 Key 方式：灵活，适合自定义数据和批量操作
3. **关键变更**：不废弃常量，而是公开承诺为稳定 API，保持 100% 向后兼容
4. 实施策略：分 4 个 Phase 进行

**Open Questions**: 无

**Resolved Questions**:
- ~~常量是否标记 @Deprecated~~ → **否**，常量保留作为公开 API
- ~~是否只保留 Helper~~ → **否**，双模式并存，用户按需选择

**References**: 
- Issue: https://github.com/alibaba/spring-ai-alibaba/issues/3110
- 方案文档: docs/issue/3110-toolcontext-metadata-solution.md
- 相关代码: spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/tools/

---

## DIA-20260131-002 | 2026-01-31 23:10 | Owner, Agent(Codex)

**Context**: 用户要求完善方案，添加一个实际的测试案例，参考现有 graph 使用模式，验证两种访问方式都能正常获取 metadata。

**Request**: 设计一个完整的测试案例，确保 Helper 方式和直接 Key 方式都能获取到相同的 metadata。

**Decisions**:
1. 创建了 `ToolContextDualAccessExample` 完整测试类
2. 设计了 `MetadataValidationTool` 工具类，同时用两种方式获取：
   - Helper 方式：`ToolContextHelper.getConfig()`, `getState()`, `getMetadata()`
   - 直接 Key 方式：`context.get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY)` 等
3. 验证点包括：Config 获取一致性、State 获取一致性、自定义 metadata 获取
4. 测试案例包含完整的 Agent 配置、工具定义和运行验证

**Deliverable**:
- 新增 "完整测试案例（双模式验证）" 章节到方案文档
- 提供可直接运行的示例代码（基于 ReactAgent + FunctionToolCallback 模式）
- 包含预期输出示例和验证表格

**Next**: 等待方案评审，确认后可进入实施阶段

---

## DIA-20260205-001 | 2026-02-05 22:45 | Owner(你), Agent(Codex)

**Context**: 需要提供一个可运行的复现用例，复现 Issue #4184（skills 工具 + HITL 审批后找不到工具回调）。

**Request**: 重写 `examples/studio-cors-conditional-it/src/main/java/.../Issue4184FullAgentTest.java`，能稳定复现异常，避免依赖外部 LLM 和硬编码 API key。

**Decisions**:
1. 用纯本地方式构造 SkillRegistry + SkillsAgentHook，确保 `read_skill` 工具存在
2. 用 HumanInTheLoopHook + InterruptionMetadata 模拟“审批已通过”阶段
3. 用空 toolCallbacks 的 AgentToolNode 模拟“Checkpoint 恢复后工具回调丢失”，触发 `No ToolCallback found ... read_skill`
4. `messages` key 必须注册 AppendStrategy，避免 HITL 生成的 RemoveByHash 残留导致 ClassCastException（保证复现聚焦在工具丢失问题）

**Open Questions**:
- 是否需要将该用例迁移为 `src/test/java` 的自动化回归测试（当前是可运行 main 的 repro）

**References**:
- Issue: https://github.com/alibaba/spring-ai-alibaba/issues/4184
- 用例: examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java
- 记录: docs/feature/feature-02-issue-4184-repro-case.md

---

## DIA-20260205-002 | 2026-02-05 23:35 | Owner(你), Agent(Codex)

**Context**: 需要比较多种 Issue #4184 修复方案，评估落点与风险。

**Request**: 产出多个修复方案并存档到 docs，便于选择。

**Decisions**:
1. 给出 4 套方案（A/B/C/D），覆盖注入链路、HITL 恢复、节点内兜底、序列化持久化等路径
2. 推荐方案 A（恢复前重新注入 tools），作为主方案候选
3. 文档补齐前端/Mock/后端/验收与测试章节

**Open Questions**:
- 方案评审后是否需要立刻落地到 ReactAgent/DefaultBuilder

**References**:
- Issue: https://github.com/alibaba/spring-ai-alibaba/issues/4184
- 方案文档: docs/feature/feature-04-issue-4184-fix-options.md

---

## DIA-20260205-002 | 2026-02-05 23:18 | Owner(你), Agent(Codex)

## DIA-20260208-001 | 2026-02-08 00:33 | Owner(你), Agent(Codex)

**Context**: Issue #4184 仍需落地“方案 A”，并用可运行用例验证修复有效。

**Request**: 修改 `SkillsAgentHook` 与 `ReactAgent`，修复 `docs/feature/feature-04-issue-4184-fix-options.md`，然后执行 `mvn install` 与 `Issue4184FullAgentTest` 验证。

**Decisions**:
1. 在 `ReactAgent` 覆盖 `doInvoke/doStream`：当 `AgentToolNode.toolCallbacks` 为空（典型于恢复后）时，从 hooks/interceptors 重新收集并注入。
2. `SkillsAgentHook` 仅持久化 skills 工具名到 state，避免持久化 `ToolCallback` 对象图。
3. `Issue4184FullAgentTest` 改为“验证用例”，通过反射清空 toolCallbacks 来模拟恢复场景，并断言 `read_skill` 可执行成功。

**Open Questions**:
- 是否需要把验证用例迁移为 `src/test/java` 的自动化回归测试（当前是 runnable main）。

**References**:
- docs/feature/feature-04-issue-4184-fix-options.md
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/ReactAgent.java
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java
- examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/Issue4184FullAgentTest.java

**Context**: 复现用例已稳定重现 Issue #4184，需提供修复方案与代码实现。

**Request**: 修改 AgentToolNode 与 SkillsAgentHook，解决 HITL 恢复后技能工具回调丢失问题。

**Decisions**:
1. SkillsAgentHook 在 state 中持久化技能工具名与 hook 实例名
2. AgentToolNode 在执行前自动恢复缺失工具，并兼容空/不可变工具列表
3. 工具仍无法恢复时，抛出一致的 No ToolCallback found 异常

**Open Questions**:
- 是否补充自动化回归测试覆盖 HITL 恢复路径

**References**:
- Issue: https://github.com/alibaba/spring-ai-alibaba/issues/4184
- 记录: docs/feature/feature-03-issue-4184-fix.md

---

## DIA-20260207-001 | 2026-02-07 20:20 | Owner(你), Agent(Codex)

**Context**: 确认撤回最小改动方案，转为按方案 B 实施修复。

**Request**: 由 HumanInTheLoopHook 在审批恢复时触发工具重注入。

**Decisions**:
1. 在 `HumanInTheLoopHook` 处理人类反馈时调用 `ReactAgent.refreshToolsForResume()`
2. `ReactAgent` 负责统一收集 tools/providers/resolver 与 hooks/interceptors 的 toolCallbacks 并注入

**Open Questions**:
- 是否补充自动化回归测试覆盖 HITL resume

**References**:
- docs/feature/feature-04-issue-4184-fix-options.md
- docs/feature/feature-05-issue-4184-hitl-hook-injection.md

---

## DIA-20260207-002 | 2026-02-07 21:34 | Owner(你), Agent(Codex)

**Context**: 代码已回退，用户要求新增测试类验证“普通工具也会在 HITL 恢复后丢失”。

**Request**: 在 PingController 所在目录新增可运行复现类，复现普通工具在恢复后找不到 ToolCallback。

**Decisions**:
1. 新增 `NormalToolHITLLossTest`，使用普通 `FunctionToolCallback`（`echo`）+ HITL 审批路径
2. 通过构造空 toolCallbacks 的 AgentToolNode 模拟 checkpoint 恢复后丢失，触发 `No ToolCallback found`
3. 同步新增 feature 文档与任务记录

**Open Questions**:
- 是否需要迁移为 `src/test/java` 的自动化回归用例

**References**:
- examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java
- docs/feature/feature-06-normal-tool-hitl-loss-repro.md

## DIA-20260207-003 | 2026-02-07 21:39 | Owner(你), Agent(Codex)

**Context**: 运行 NormalToolHITLLossTest 时报 `inputType cannot be null`。

**Request**: 修复 FunctionToolCallback 的 inputType 配置，确保示例可运行。

**Decisions**:
1. 为 echo 工具补充 `inputType(String.class)`
2. 同步更新 feature 文档与任务记录

**Open Questions**: 无

**References**:
- examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java
- docs/feature/feature-06-normal-tool-hitl-loss-repro.md

---

## DIA-20260207-004 | 2026-02-07 21:45 | Owner(你), Agent(Codex)

**Context**: 评估 NormalToolHITLLossTest 是否为“虚假复现”，并希望打破该用例验证真实行为。

**Request**: 调整示例，保留工具回调而非手动清空，验证 HITL 恢复后工具仍可正常执行。

**Decisions**:
1. 将恢复后的 `AgentToolNode` 保留 `echo` 工具回调
2. 期望执行成功而非抛出 `No ToolCallback found`
3. 同步更新 feature 文档与历史记录

**Open Questions**:
- 是否需要补充真正的 checkpoint/restore 流程来验证工具持久化

**References**:
- examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/corsit/NormalToolHITLLossTest.java
- docs/feature/feature-06-normal-tool-hitl-loss-repro.md

---

## DIA-20260207-005 | 2026-02-07 21:54 | Owner(你), Agent(Codex)

**Context**: 需要一个参考 HITL 文档的可运行示例，验证人工决策后工具是否丢失。

**Request**: 编写一个带 tools 的 HITL 案例，并验证 HITL 决策后 Tool 是否会丢失。

**Decisions**:
1. 新增独立可运行示例 `HITLToolRetentionCheck`
2. 用 `HumanInTheLoopHook` + `AgentToolNode` 模拟审批后执行工具
3. 运行时若抛出 `No ToolCallback found` 视为丢失，否则视为保留

**Open Questions**:
- 是否需要补充真实 checkpoint/restore 流程的验证

**References**:
- examples/studio-cors-conditional-it/src/main/java/com/alibaba/cloud/ai/examples/studiocorsconditionalit/HITLToolRetentionCheck.java
- docs/feature/feature-07-hitl-tool-retention-check.md

---

## DIA-20260207-006 | 2026-02-07 23:01 | Owner(你), Agent(Codex)

**Context**: HITL 恢复后 skills 工具回调丢失，需采用方案 A 让 Skills 机制自行管理激活状态。

**Request**: 按方案 A 修改 SkillsAgentHook 与 SkillsInterceptor，保存并恢复 skill 上下文。

**Decisions**:
1. SkillsAgentHook 改为 ModelHook，在 before/after model 持久化已激活 skill 名称列表
2. beforeModel 合并 persisted + message scan 结果，并写入 ModelRequest metadata
3. SkillsInterceptor 优先读取 metadata，缺失时回退到消息历史扫描

**Open Questions**:
- 是否需要新增 HITL resume 回归测试覆盖 skills 激活恢复路径

**References**:
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/interceptor/skills/SkillsInterceptor.java
- docs/feature/feature-08-issue-4184-skills-context-retention.md

## DIA-20260208-002 | 2026-02-08 10:07 | Owner(你), Agent(Codex)

**Context**: 用户询问我们目前做了什么工作，并询问下一步。

**Request**: 总结已完成的工作（4个sandbox示例），修复sandbox-custom中的编译错误，并更新文档。

**Decisions**:
1. 已创建4个完整的sandbox示例模块：
   - ✅ sandbox-simple-tool: REST API与多种工具（Calculator, Weather, Python, Shell, Browser）
   - ✅ sandbox-structured-output: 结构化数据提取（Browser + POJO）
   - ✅ sandbox-custom: 自定义Sandbox实现（SPI机制 + @RegisterSandbox）
   - ✅ sandbox-browser-fullstack: 全栈应用（React前端 + WebSocket + SSE）

2. 已修复sandbox-custom中的API兼容性问题：
   - 替换不存在的`PythonSandboxTool`类，使用`SaaBasePythonRunner`
   - 修复`sandbox.execute()`调用，改用`pythonRunner.apply()`
   - 修复`SandboxProvider`接口实现，更新`getSandboxClasses()`返回类型
   - 移除使用不存在`executeCommand()`方法的代码

3. 所有4个示例均已成功编译

**Verification**:
- sandbox-simple-tool: COMPILES ✅
- sandbox-structured-output: COMPILES ✅
- sandbox-custom: COMPILES ✅（已修复API问题）
- sandbox-browser-fullstack: COMPILES ✅

**References**:
- examples/sandbox-simple-tool/
- examples/sandbox-structured-output/
- examples/sandbox-custom/
- examples/sandbox-browser-fullstack/
- docs/feature.csv
- docs/_history.md

---

| CHG-20260208-007 | 2026-02-08 22:35 | devops | 13 | sandbox-examples | 为所有示例添加.dockerignore文件优化构建上下文，更新README添加Docker测试指南 | examples/sandbox-simple-tool/.dockerignore; examples/sandbox-structured-output/.dockerignore; examples/sandbox-custom/.dockerignore; examples/sandbox-browser-fullstack/backend/.dockerignore; examples/sandbox-browser-fullstack/frontend/.dockerignore; examples/README.md | 低 | 所有.dockerignore文件已创建；README已更新Docker部署架构图、测试指南、故障排除 | Docker测试因环境镜像问题待后续验证 |
| CHG-20260208-012 | 2026-02-08 19:35 | backend | 11 | issue-4184 | 修复DefaultBuilder自动配置SkillToolCallbackResolver以完善Issue #4184修复 | spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java | 中（构建器工具解析器配置） | Issue4184FullAgentTest验证通过：技能工具从状态恢复并执行成功 | 无 |

---

## DIA-20260213-001 | 2026-02-13 10:35 | Owner(你), Agent(Codex)

**Context**: PR review 提出 DefaultBuilder/SkillToolCallbackResolver 的结构与一致性问题。

**Request**: 直接修复除测试外的所有问题（移除 break、避免重复解析 resolver、统一 read_skill 常量）。

**Decisions**:
1. 移除 SkillsAgentHook 处理中的 break，支持多个 hook 参与 resolver 链。
2. ToolCallbackResolver 在 build() 中只解析一次，gatherLocalTools 不再重复解析。
3. read_skill 工具名常量统一引用 ReadSkillTool.READ_SKILL。

**Open Questions**:
- 是否需要补充 resolver 链式组合与 read_skill 恢复的单测（后续单独处理）。

**References**:
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java
- spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillToolCallbackResolver.java
- docs/feature/feature-15-defaultbuilder-resolver-pr-fixes.md
