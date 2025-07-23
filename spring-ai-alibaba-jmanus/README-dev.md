# Spring AI Alibaba JManus 开发者快速入门

本项目是阿里巴巴 AI 计划执行与管理系统，采用 Spring Boot 架构，支持计划生成、异步执行、状态跟踪、配置管理等功能。以下为开发者调试和理解系统的核心入口与接口说明。

## 1. 核心模块接口入口

- 所有核心 REST API 均在 `src/main/java/com/alibaba/cloud/ai/example/manus/planning/controller/` 及 `config/` 目录下。
- 推荐从 `ManusController.java`、`PlanTemplateController.java`、`ConfigController.java` 入手，快速定位主要业务流程。

## 2. 任务发起与处理

### 任务发起

- 接口文件：`ManusController.java`
- 主要入口：`/api/executor/execute`（POST）
  - 参数：`query`（用户查询内容）
  - 功能：发起一个新的计划任务，系统自动分配 planId，异步执行。

### 任务处理

- 计划任务由 `PlanningFactory`、`PlanningCoordinator` 负责调度与执行。
- 执行上下文由 `ExecutionContext` 管理，包含用户请求、计划ID、是否需要摘要等信息。

## 3. 任务异步状态跟踪

- 接口文件：`ManusController.java`
- 主要入口：
  - `/api/executor/details/{planId}`（GET）：获取指定 planId 的详细执行记录及状态。
- 辅助接口
  - `/api/executor/details/{planId}`（DELETE）：删除指定 planId 的执行记录。
  - `/api/executor/submit-input/{planId}`（POST）：提交用户输入（如任务等待用户表单填写时）。

- 状态跟踪说明：
  - 任务发起后返回 `planId`，可通过上述接口查询任务进度、结果或提交补充信息。 你可以详细查看 Execution Record这个对象。 我们的设计理念是所有的反馈都用着一个对象以简化被集成操作。里面有详细注释
  - 支持用户输入等待与合并，便于交互式任务流。

## 4. 配置模块

- 接口文件：`ConfigController.java`
- 主要入口：
  - `/api/config/group/{groupName}`（GET）：按分组获取配置项。
  - `/api/config/batch-update`（POST）：批量更新配置项。

- 配置实体：`ConfigEntity`
- 业务服务：`ConfigService`

## 5. plan-act模式核心接口

- 接口文件：`PlanTemplateController.java`
- 主要入口：
  - `/api/plan-template/generate`（POST）：生成新的计划模板。
  - `/api/plan-template/executePlanByTemplateId`（POST/GET）：按模板ID执行计划。
  - `/api/plan-template/save`（POST）：保存计划版本。
  - `/api/plan-template/versions`（POST）：获取计划版本历史。
  - `/api/plan-template/get-version`（POST）：获取指定版本计划。
  - `/api/plan-template/list`（GET）：获取所有计划模板列表。
  - `/api/plan-template/update`（POST）：更新计划模板。
  - `/api/plan-template/delete`（POST）：删除计划模板。

- 计划生成、版本管理、执行均通过 `PlanTemplateService`、`PlanIdDispatcher`、`PlanningFactory` 协作完成。

## 6. 快速调试建议

- 启动服务后，推荐使用 Postman 或 curl 直接调用上述接口，观察返回结果和任务状态。
- 通过 `/api/executor/details/{planId}` 跟踪异步任务进度。
- 修改配置可用 `/api/config/batch-update`，无需重启服务。
- 计划模板相关操作均可通过 `/api/plan-template/*` 系列接口完成。

---

如需更详细的接口参数、返回结构或业务流程，请参考对应 Controller 源码及 Service 层实现。
