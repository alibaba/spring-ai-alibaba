# Skills Registry Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add additive skill-level allowed tools, path-aware `read_skill`, local registry search, and current-registry disable support without breaking the existing skills flow.

**Architecture:** Keep the current `SkillsAgentHook -> SkillsInterceptor -> dynamicToolCallbacks` pipeline and extend it incrementally. Add new registry APIs in graph-core, add path/search/disable tools in agent-framework, and centralize tool de-duplication by tool name so static and dynamic registrations behave consistently.

**Tech Stack:** Java 17, Spring AI tool callbacks, Spring AI Alibaba ReactAgent framework, JUnit 5, Maven

---

### Task 1: Extend skill metadata and registry behavior in graph-core

**Files:**
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillMetadata.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/SkillRegistry.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/AbstractSkillRegistry.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/SkillScanner.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistry.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/classpath/ClasspathSkillRegistry.java`
- Test: `spring-ai-alibaba-graph-core/src/test/java/com/alibaba/cloud/ai/graph/skills/registry/classpath/ClasspathSkillRegistryTest.java`
- Create: `spring-ai-alibaba-graph-core/src/test/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistryEnhancementsTest.java`

- [ ] **Step 1: Write failing registry tests for allowed tools, path lookup, search, and disable**

```java
@Test
void searchMatchesNameDescriptionAndPath() {
    FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
        .projectSkillsDirectory(skillsDir.toString())
        .build();

    assertTrue(registry.search("pdf").stream().anyMatch(skill -> "pdf-extractor".equals(skill.getName())));
}

@Test
void disableHidesSkillFromReadsAndSearch() {
    assertTrue(registry.disable("pdf-extractor"));
    assertFalse(registry.contains("pdf-extractor"));
    assertTrue(registry.search("pdf").isEmpty());
    assertThrows(IllegalStateException.class, () -> registry.readSkillContent("pdf-extractor"));
}
```

- [ ] **Step 2: Run tests to verify the new registry API is missing**

Run: `./mvnw -pl :spring-ai-alibaba-graph-core -Dtest=FileSystemSkillRegistryEnhancementsTest,ClasspathSkillRegistryTest test`

Expected: compilation failures or failing assertions around missing `search`, `disable`, `getByPath`, `readSkillContentByPath`, and `allowedTools`.

- [ ] **Step 3: Add `allowedTools` to `SkillMetadata` and parse frontmatter variants in `SkillScanner`**

```java
private List<String> allowedTools = List.of();

public List<String> getAllowedTools() {
    return allowedTools;
}

public void setAllowedTools(List<String> allowedTools) {
    this.allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
}
```

```java
Object allowedToolsValue = frontmatter.containsKey("allowed_tools")
    ? frontmatter.get("allowed_tools")
    : frontmatter.get("allowedTools");
List<String> allowedTools = normalizeAllowedTools(allowedToolsValue);
builder.allowedTools(allowedTools);
```

- [ ] **Step 4: Extend `SkillRegistry` and implement disable/search/path support centrally**

```java
Optional<SkillMetadata> getByPath(String skillPath);
String readSkillContentByPath(String skillPath) throws IOException;
List<SkillMetadata> search(String query);
boolean disable(String name);
boolean disableByPath(String skillPath);
boolean isDisabled(String name);
```

```java
protected final Set<String> disabledSkillNames = ConcurrentHashMap.newKeySet();

public List<SkillMetadata> listAll() {
    return skills.values().stream()
        .filter(skill -> !disabledSkillNames.contains(skill.getName()))
        .sorted(Comparator.comparing(SkillMetadata::getName))
        .toList();
}
```

- [ ] **Step 5: Implement filesystem and classpath path-based reads plus normalized path matching**

```java
private static String normalizePath(String skillPath) {
    return Path.of(skillPath).toAbsolutePath().normalize().toString();
}
```

```java
public Optional<SkillMetadata> getByPath(String skillPath) {
    String normalized = normalizePath(skillPath);
    return listAll().stream()
        .filter(skill -> normalized.equals(normalizePath(skill.getSkillPath())))
        .findFirst();
}
```

- [ ] **Step 6: Re-run graph-core tests**

Run: `./mvnw -pl :spring-ai-alibaba-graph-core -Dtest=FileSystemSkillRegistryEnhancementsTest,ClasspathSkillRegistryTest test`

Expected: PASS for the new registry behaviors.

- [ ] **Step 7: Commit the graph-core registry changes**

```bash
git add \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillMetadata.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/SkillRegistry.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/AbstractSkillRegistry.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/SkillScanner.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistry.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/classpath/ClasspathSkillRegistry.java \
  spring-ai-alibaba-graph-core/src/test/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistryEnhancementsTest.java \
  spring-ai-alibaba-graph-core/src/test/java/com/alibaba/cloud/ai/graph/skills/registry/classpath/ClasspathSkillRegistryTest.java
git commit -m "feat(skill): add registry search and disable support"
```

### Task 2: Add path/search/disable skill tools in agent-framework

**Files:**
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/ReadSkillTool.java`
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java`
- Create: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SearchSkillsTool.java`
- Create: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/DisableSkillTool.java`
- Test: `spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/AgentSkillsTest.java`

- [ ] **Step 1: Write failing agent-framework tests for path reads, search, and disable**

```java
@Test
void readSkillToolSupportsSkillPath() {
    String content = tool.apply(new ReadSkillRequest(null, skillPath), new ToolContext(Map.of()));
    assertTrue(content.contains("Grouped Tools Test Skill"));
}

@Test
void disableSkillToolHidesSkillFromSearch() {
    disableTool.apply(new DisableSkillRequest("grouped-tools-test", null), context);
    String result = searchTool.apply(new SearchSkillsRequest("grouped"), context);
    assertFalse(result.contains("grouped-tools-test"));
}
```

- [ ] **Step 2: Run tests to verify the tools do not exist or do not support the new inputs**

Run: `./mvnw -pl :spring-ai-alibaba-agent-framework -Dtest=AgentSkillsTest test`

Expected: failures around missing request fields, missing tool classes, or unchanged hook tool list.

- [ ] **Step 3: Extend `ReadSkillTool` to resolve by name or path**

```java
if (!StringUtils.hasText(request.skillName) && !StringUtils.hasText(request.skillPath)) {
    return "Error: skill_name or skill_path is required";
}
```

```java
String content = StringUtils.hasText(request.skillPath)
    ? skillRegistry.readSkillContentByPath(request.skillPath)
    : skillRegistry.readSkillContent(request.skillName);
```

- [ ] **Step 4: Add `SearchSkillsTool` and `DisableSkillTool` with compact deterministic output**

```java
return matches.stream()
    .map(skill -> "- %s | %s | %s | allowed_tools=%s".formatted(
        skill.getName(), skill.getSource(), skill.getSkillPath(), skill.getAllowedTools()))
    .collect(Collectors.joining("\n"));
```

- [ ] **Step 5: Register the new tools in `SkillsAgentHook`**

```java
public List<ToolCallback> getTools() {
    return List.of(readSkillTool, searchSkillsTool, disableSkillTool);
}
```

- [ ] **Step 6: Re-run agent-framework tests**

Run: `./mvnw -pl :spring-ai-alibaba-agent-framework -Dtest=AgentSkillsTest test`

Expected: PASS for path reads, hook tool exposure, search, and disable flows.

- [ ] **Step 7: Commit the skill tool changes**

```bash
git add \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/ReadSkillTool.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SearchSkillsTool.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/DisableSkillTool.java \
  spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/AgentSkillsTest.java
git commit -m "feat(skill): add read search and disable tools"
```

### Task 3: Wire metadata-driven tool activation and centralized de-duplication

**Files:**
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/interceptor/skills/SkillsInterceptor.java`
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java`
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java`
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/node/AgentLlmNode.java`
- Test: `spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/AgentSkillsTest.java`
- Test: `spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/interceptors/ModelRequestTest.java`

- [ ] **Step 1: Add failing tests for additive allowed tools and tool-name de-duplication**

```java
@Test
void allowedToolsAreResolvedAndAddedWithoutRemovingStaticTools() {
    // agent has a static python tool, skill allows record_result
    // after read_skill both python and record_result should be available
}

@Test
void duplicateToolNamesAreCollapsed() {
    assertEquals(1, uniqueByName(toolCallbacks).size());
}
```

- [ ] **Step 2: Run tests to verify duplicate handling and metadata-based activation are missing**

Run: `./mvnw -pl :spring-ai-alibaba-agent-framework -Dtest=AgentSkillsTest,ModelRequestTest test`

Expected: failures showing no metadata-driven activation and duplicate tools remaining in merged lists.

- [ ] **Step 3: Add optional resolver-backed activation for `SkillMetadata.allowedTools`**

```java
private List<ToolCallback> resolveAllowedTools(SkillMetadata skill) {
    return skill.getAllowedTools().stream()
        .map(this.toolCallbackResolver::resolve)
        .filter(Objects::nonNull)
        .toList();
}
```

- [ ] **Step 4: Update `SkillsInterceptor` to parse read history by name or path and skip disabled skills**

```java
record ReadSkillReference(String skillName, String skillPath) {}
```

```java
SkillMetadata skill = resolveSkillReference(reference);
if (skill == null || skillRegistry.isDisabled(skill.getName())) {
    continue;
}
```

- [ ] **Step 5: Centralize tool de-duplication by `toolDefinition.name()`**

```java
private static List<ToolCallback> deduplicateToolCallbacks(List<ToolCallback> toolCallbacks) {
    Map<String, ToolCallback> unique = new LinkedHashMap<>();
    for (ToolCallback callback : toolCallbacks) {
        unique.putIfAbsent(callback.getToolDefinition().name(), callback);
    }
    return new ArrayList<>(unique.values());
}
```

- [ ] **Step 6: Apply the de-dup helper in builder, interceptor, and LLM-node merge points**

Run these commands after implementation:

`./mvnw -pl :spring-ai-alibaba-agent-framework -Dtest=AgentSkillsTest,ModelRequestTest test`

Expected: PASS with grouped tools still working, metadata-driven tools added additively, and duplicates collapsed.

- [ ] **Step 7: Commit the activation and de-dup changes**

```bash
git add \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/interceptor/skills/SkillsInterceptor.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/SkillsAgentHook.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/DefaultBuilder.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/node/AgentLlmNode.java \
  spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/AgentSkillsTest.java \
  spring-ai-alibaba-agent-framework/src/test/java/com/alibaba/cloud/ai/graph/agent/interceptors/ModelRequestTest.java
git commit -m "feat(skill): add allowed tools activation"
```

### Task 4: Update prompts, docs, and run targeted verification

**Files:**
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillPromptConstants.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistry.java`
- Modify: `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SpringAiSkillAdvisor.java`
- Modify: `spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/ReadSkillTool.java`
- Modify: `docs/superpowers/specs/2026-03-28-skills-registry-enhancements-design.md`

- [ ] **Step 1: Adjust prompt and Javadoc wording so `read_skill` no longer implies name-only or path-only access**

```java
"Use `read_skill` with either the registered skill name or the skill path shown in the list."
```

- [ ] **Step 2: Run focused module tests**

Run: `./mvnw -pl :spring-ai-alibaba-graph-core,:spring-ai-alibaba-agent-framework -Dtest=FileSystemSkillRegistryEnhancementsTest,ClasspathSkillRegistryTest,AgentSkillsTest,ModelRequestTest test`

Expected: PASS for all new registry and agent skill coverage.

- [ ] **Step 3: Run one package pass per module if targeted tests passed**

Run: `./mvnw -pl :spring-ai-alibaba-graph-core,:spring-ai-alibaba-agent-framework test -DskipITs`

Expected: PASS, or unrelated pre-existing failures only.

- [ ] **Step 4: Inspect the final diff before closing**

Run: `git status --short`  
Expected: only intended files changed.

Run: `git diff --stat`  
Expected: skill registry, hook/interceptor, tests, and docs only.

- [ ] **Step 5: Commit the verification and doc wording updates**

```bash
git add \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SkillPromptConstants.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/registry/filesystem/FileSystemSkillRegistry.java \
  spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/skills/SpringAiSkillAdvisor.java \
  spring-ai-alibaba-agent-framework/src/main/java/com/alibaba/cloud/ai/graph/agent/hook/skills/ReadSkillTool.java \
  docs/superpowers/specs/2026-03-28-skills-registry-enhancements-design.md
git commit -m "docs(skill): clarify path-aware skill loading"
```
