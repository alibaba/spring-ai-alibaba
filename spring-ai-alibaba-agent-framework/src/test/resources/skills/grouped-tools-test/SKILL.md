---
name: grouped-tools-test
description: Test skill for groupedTools. When executing this skill, use the record_result tool to record the result value.
---

# Grouped Tools Test Skill

This skill is used to verify that tools registered via groupedTools are available and used after the agent reads this skill with `read_skill`.

## Instructions

When the user asks you to complete a task according to this skill:

1. Use the `record_result` tool to record the result value provided by the user.
2. The tool accepts a parameter "value" (string). Pass the value the user asked to record.
3. After calling the tool successfully, confirm to the user that the result was recorded.

## Example

User: "按 grouped-tools-test 技能要求，用 record_result 记录：hello"
Action: Call record_result with value "hello", then reply that the result was recorded.
