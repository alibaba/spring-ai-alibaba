# PlanExecutionRecord JSON Format Description

This document describes the JSON output format of PlanExecutionRecord and its field descriptions.

## Data Structure Overview

PlanExecutionRecord contains four main parts:

1. Basic Info
2. Plan Structure
3. Execution Data
4. Execution Result

## Field Descriptions

### Basic Information

- `id`: Unique identifier of the record (Long type)
- `planId`: Unique identifier of the plan (String type)
- `title`: Plan title
- `userRequest`: User's original request
- `startTime`: Execution start time (ISO-8601 format)
- `endTime`: Execution end time (ISO-8601 format)

### Execution Status

- `currentStepIndex`: Current step index being executed (starting from 0)
- `completed`: Whether completed (boolean type)
- `summary`: Execution summary or current status description

### Step Information

- `steps`: List of plan steps, each step contains agent identifier
  
### Agent Execution Records

- `agentExecutionSequence`: List of agent execution records
  - `id`: Execution record ID
  - `conversationId`: Conversation ID
  - `agentName`: Agent name
  - `agentDescription`: Agent description
  - `startTime`: Start time
  - `endTime`: End time
  - `maxSteps`: Maximum number of steps
  - `currentStep`: Current step
  - `status`: Execution status
  - `isCompleted`: Whether completed
  - `isStuck`: Whether stuck
  - `agentRequest`: Agent request
  - `result`: Execution result
  - `thinkActSteps`: Think and act steps list

## JSON Structure Example

```json
{
    "id": 1711624451711,
    "planId": "plan_1743142451689",
    "title": "Plan for: Query the latest Alibaba stock price through Baidu",
    "userRequest": "Query the latest Alibaba stock price through Baidu",
    "startTime": "2025-03-28T14:14:11.711141",
    "endTime": "2025-03-28T14:14:45.324512",
    "currentStepIndex": 2,
    "completed": false,
    "summary": "In progress...",
    "steps": [
        "[BROWSER_AGENT] Open Baidu search page",
        "[BROWSER_AGENT] Search for Alibaba stock price information",
        "[REACT_AGENT] Analyze and extract stock price data"
    ],
    "agentExecutionSequence": [
        {
            "id": 1711624451712,
            "conversationId": "plan_1743142451689",
            "agentName": "BROWSER_AGENT",
            "agentDescription": "Web browsing agent",
            "startTime": "2025-03-28T14:14:11.712141",
            "endTime": "2025-03-28T14:14:15.324512",
            "maxSteps": 3,
            "currentStep": 1,
            "status": "COMPLETED",
            "isCompleted": true,
            "isStuck": false,
            "agentRequest": "Open Baidu search page",
            "result": "Successfully opened Baidu homepage",
            "thinkActSteps": [
                {
                    "id": 1711624451713,
                    "parentExecutionId": 1711624451712,
                    "thinkStartTime": "2025-03-28T14:14:11.713141",
                    "thinkEndTime": "2025-03-28T14:14:12.324512",
                    "actStartTime": "2025-03-28T14:14:12.324512",
                    "actEndTime": "2025-03-28T14:14:15.324512",
                    "thinkInput": "Need to open Baidu search page",
                    "thinkOutput": "Use browser to open Baidu homepage",
                    "actionNeeded": true,
                    "actionDescription": "Open browser and navigate to Baidu homepage",
                    "actionResult": "Successfully accessed https://www.baidu.com",
                    "status": "completed",
                    "toolName": "browser",
                    "toolParameters": "{\"url\": \"https://www.baidu.com\"}"
                }
            ]
        }
    ]
}
```

## Usage Instructions

1. All timestamps use ISO-8601 format
2. Status values include: COMPLETED, IN_PROGRESS, PENDING, ERROR, etc.
3. Agent name format is uppercase with underscores, e.g., BROWSER_AGENT
4. Tool parameters are stored in JSON string format

## Best Practices

1. Always check the `completed` field to determine if execution is complete
2. Use `currentStepIndex` and `steps.length` to calculate progress
