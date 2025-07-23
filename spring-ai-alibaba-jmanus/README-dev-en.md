# Spring AI Alibaba JManus Developer Quick Start

This project is Alibaba's AI plan execution and management system, built with Spring Boot. It supports plan generation, asynchronous execution, status tracking, and configuration management. Below is a guide for developers to quickly debug and understand the core entry points and interfaces.

## 1. Core Module API Entry Points

- All core REST APIs are located in `src/main/java/com/alibaba/cloud/ai/example/manus/planning/controller/` and `config/` directories.
- Recommended starting points: `ManusController.java`, `PlanTemplateController.java`, and `ConfigController.java` for the main business logic.

## 2. Task Initiation and Processing

### Task Initiation

- API File: `ManusController.java`
- Main Endpoint: `/api/executor/execute` (POST)
  - Parameter: `query` (user query content)
  - Function: Initiates a new plan task, system automatically assigns a planId, and executes asynchronously.

### Task Processing

- Plan tasks are scheduled and executed by `PlanningFactory` and `PlanningCoordinator`.
- Execution context is managed by `ExecutionContext`, which contains user request, plan ID, and summary requirements.

## 3. Asynchronous Task Status Tracking

- API File: `ManusController.java`
- Main Endpoint:
  - `/api/executor/details/{planId}` (GET): Get detailed execution record and status for the specified planId.
- Auxiliary Endpoints:
  - `/api/executor/details/{planId}` (DELETE): Delete execution record for the specified planId.
  - `/api/executor/submit-input/{planId}` (POST): Submit user input (for tasks waiting for user form input).

- Status Tracking Notes:
  - After task initiation, the returned `planId` can be used to query task progress, results, or submit additional information. You can inspect the Execution Record object in detail. Our design philosophy is to use a unified object for all feedback to simplify integration. The object is well documented.
  - Supports user input waiting and merging, enabling interactive task flows.

## 4. Configuration Module

- API File: `ConfigController.java`
- Main Endpoints:
  - `/api/config/group/{groupName}` (GET): Get configuration items by group.
  - `/api/config/batch-update` (POST): Batch update configuration items.

- Configuration Entity: `ConfigEntity`
- Service: `ConfigService`

## 5. Core Interfaces for Plan-Act Pattern

- API File: `PlanTemplateController.java`
- Main Endpoints:
  - `/api/plan-template/generate` (POST): Generate a new plan template.
  - `/api/plan-template/executePlanByTemplateId` (POST/GET): Execute plan by template ID.
  - `/api/plan-template/save` (POST): Save plan version.
  - `/api/plan-template/versions` (POST): Get plan version history.
  - `/api/plan-template/get-version` (POST): Get a specific version of a plan.
  - `/api/plan-template/list` (GET): Get all plan templates.
  - `/api/plan-template/update` (POST): Update plan template.
  - `/api/plan-template/delete` (POST): Delete plan template.

- Plan generation, version management, and execution are coordinated by `PlanTemplateService`, `PlanIdDispatcher`, and `PlanningFactory`.

## 6. Quick Debugging Tips

- After starting the service, use Postman or curl to call the above endpoints and observe the returned results and task status.
- Track asynchronous task progress via `/api/executor/details/{planId}`.
- Use `/api/config/batch-update` to modify configuration without restarting the service.
- All plan template operations can be performed via the `/api/plan-template/*` endpoints.

---

For more detailed interface parameters, return structures, or business logic, please refer to the corresponding Controller source code and Service layer implementations.
