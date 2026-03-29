# Skills Registry Enhancements Design

**Date:** 2026-03-28

**Status:** Draft for review

**Goal:** Extend the current Skills integration so a skill can expose incremental allowed tools, be read by name or path, be searched within the local registry, and be disabled from the current registry instance without deleting files.

## Background

The current skills flow in `spring-ai-alibaba-agent-framework` already supports:

- registry-backed skill discovery and prompt injection
- `read_skill`
- `groupedTools` mapped by skill name
- dynamic tool injection via `dynamicToolCallbacks`

The missing pieces are:

1. skill-level `allowed tools` metadata
2. `read_skill` support for path-based addressing
3. local skill search
4. current-registry disable/unregister support
5. consistent de-duplication when static and dynamic tools are merged

## Requirements

### Functional

1. `allowed tools` are additive only.
   Existing agent tools remain visible. Reading a skill can add more tools, but must not hide or restrict already-registered tools.
2. `read_skill` must support either `skill_name` or a skill path.
3. The registry must support local search over skill name, description, and path.
4. A skill can be disabled only in the current `SkillRegistry` instance.
   Disabling must not delete or modify files on disk.
5. Disabled skills must disappear from:
   - `listAll`
   - `get`
   - `contains`
   - prompt injection
   - search results
   - future dynamic tool activation
6. Dynamic and static tools must be de-duplicated by tool name.

### Non-Goals

1. No semantic search in this iteration.
2. No filesystem deletion or uninstall workflow.
3. No tool visibility restriction model.
4. No cross-registry federation.

## Proposed Design

### 1. Skill metadata

Extend `SkillMetadata` with:

- `List<String> allowedTools`

`SkillScanner` will parse this from `SKILL.md` frontmatter, supporting both:

- `allowed_tools`
- `allowedTools`

Normalization rules:

- missing field => empty list
- blank entries removed
- preserve declaration order
- no de-duplication beyond exact string equality within the metadata list

### 2. Registry API

Extend `SkillRegistry` with:

- `Optional<SkillMetadata> getByPath(String skillPath)`
- `String readSkillContentByPath(String skillPath) throws IOException`
- `List<SkillMetadata> search(String query)`
- `boolean disable(String name)`
- `boolean disableByPath(String skillPath)`
- `boolean isDisabled(String name)`

Search behavior:

- case-insensitive substring match
- fields: `name`, `description`, `skillPath`
- result order:
  1. exact name match
  2. prefix name match
  3. substring name match
  4. description/path match
  5. stable tie-break by name

Disable behavior:

- scoped to the current registry instance only
- implemented in `AbstractSkillRegistry` with an in-memory disabled set
- survives `reload()` for the lifetime of that registry instance
- a disabled skill behaves as if it were absent

### 3. Tooling additions

Keep `read_skill` and add two registry tools:

1. `search_skills`
   Inputs:
   - `query`

   Output:
   - compact structured text containing `name`, `description`, `skill_path`, `source`, and `allowed_tools`

2. `disable_skill`
   Inputs:
   - `skill_name` optional
   - `skill_path` optional

   Output:
   - success/failure message

`SkillsAgentHook.getTools()` will return:

- `read_skill`
- `search_skills`
- `disable_skill`

### 4. `read_skill` resolution rules

Update `ReadSkillTool.ReadSkillRequest` to accept:

- `skill_name`
- `skill_path`

Resolution:

1. If only `skill_name` is provided, resolve by name.
2. If only `skill_path` is provided, resolve by path.
3. If both are provided, they must resolve to the same skill.
4. If neither is provided, return validation error.
5. If the target skill is disabled, return not found semantics.

Backward compatibility:

- existing `skill_name` callers continue to work unchanged

### 5. Dynamic tool activation

Current behavior only supports `groupedTools` keyed by skill name. We will keep that path and add metadata-driven activation.

Activation sources after a skill is read:

1. `groupedTools.get(skillName)`
2. `SkillMetadata.allowedTools`, resolved to `ToolCallback`

Resolution strategy for `allowedTools`:

- add an optional tool resolver source to `SkillsAgentHook` and `SkillsInterceptor`
- preferred source is `ToolCallbackResolver`
- if absent, only `groupedTools` activation works and unresolved `allowedTools` are skipped with debug logging

This avoids forcing a large refactor in `ReactAgent` or `DefaultBuilder`.

### 6. Tool de-duplication

De-duplication key:

- `ToolCallback.getToolDefinition().name()`

Apply this in three places:

1. `DefaultBuilder.gatherLocalTools()`
   Prevent duplicate static tool registration across hooks, interceptors, providers, and direct tools.
2. `SkillsInterceptor`
   When merging existing `dynamicToolCallbacks` with grouped tools and metadata-resolved tools.
3. `AgentLlmNode.buildChatClientRequestSpec()`
   Before passing final tool callbacks to model options.

Rule:

- first occurrence wins
- later duplicates are ignored

This keeps behavior stable and avoids replacing an explicitly registered tool implementation with a later dynamic duplicate.

### 7. Prompt and visibility behavior

`buildSkillsPrompt(...)` should only include enabled skills.

Prompt text remains progressive-disclosure based, but it should no longer imply that only a path can be used to read a skill. Wording should be updated to reflect that `read_skill` supports name or path.

### 8. Search and disable interaction with prompt/history

If a skill was read earlier in the conversation and is later disabled:

- future prompt injection excludes it
- future search results exclude it
- future `read_skill` calls fail for it
- future dynamic tool injection from that skill is skipped

Historical messages remain untouched.

## Compatibility

### Backward compatible

1. Existing `read_skill(skill_name)` behavior
2. Existing `groupedTools` maps
3. Existing skill frontmatter without `allowed_tools`
4. Existing registries that do not rely on search/disable APIs

### Behavior changes

1. Duplicate tool registrations are now collapsed by tool name.
2. Disabled skills are hidden from all normal registry reads.

## Files Expected To Change

### Core registry and metadata

- `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillMetadata.java`
- `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/SkillRegistry.java`
- `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/AbstractSkillRegistry.java`
- `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/SkillScanner.java`
- `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillPromptConstants.java`

### Agent-side hooks and interceptors

- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/ReadSkillTool.java`
- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java`
- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/interceptor/skills/SkillsInterceptor.java`
- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java`
- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/node/AgentLlmNode.java`

### New tool classes

- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SearchSkillsTool.java`
- `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/DisableSkillTool.java`

### Tests

- `spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/AgentSkillsTest.java`
- `spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/interceptors/ModelRequestTest.java`
- new registry tests under `spring-ai-alibaba-graph-core/src/test/...`

## Testing Strategy

1. Parse `allowed_tools` and `allowedTools` frontmatter variants.
2. Read a skill by name.
3. Read a skill by path.
4. Fail when name/path disagree.
5. Search by name, description, and path.
6. Disable by name and by path.
7. Confirm disabled skills disappear from prompt-visible lists.
8. Confirm disabled skills can no longer activate dynamic tools.
9. Confirm dynamic and static tool merges de-duplicate by tool name.
10. Confirm grouped tools remain functional.

## Risks

1. `allowedTools` resolution depends on access to a `ToolCallbackResolver` or equivalent source.
   Mitigation: make this optional and preserve `groupedTools` as the guaranteed path.
2. Some callers may implicitly rely on duplicate tool entries.
   Mitigation: de-dup by tool name consistently and add targeted tests.
3. Path matching can become brittle if callers pass non-normalized paths.
   Mitigation: normalize with `Path.of(...).toAbsolutePath().normalize()` before comparison where applicable.

## Implementation Notes

1. Keep the design additive and avoid changing the overall ReactAgent construction model.
2. Prefer filtering disabled skills centrally in `AbstractSkillRegistry` instead of teaching each caller about disabled state.
3. Use the existing `groupedTools` concept as the compatibility anchor.
4. Update docs and inline Javadoc where they currently imply `read_skill` only works by name or only by path.
