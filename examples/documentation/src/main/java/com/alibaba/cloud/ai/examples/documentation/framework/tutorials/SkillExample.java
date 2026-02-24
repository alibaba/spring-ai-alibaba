/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Skills Tutorial - 完整代码示例
 * 展示如何使用 SkillRegistry 管理技能、渐进式工具披露以及 Shell 集成
 *
 * @author zlt
 */
public class SkillExample {
    // ==================== 基础注册表使用 ====================

    /**
     * 示例1：使用文件系统加载技能
     * 适用于技能文件存储在本地磁盘的场景
     */
    public static SkillRegistry fileSystemSkillRegistry() {
        // 配置从当前项目的 skills 目录加载
        // 目录结构示例: ./skills/my-skill/SKILL.md （每个技能是一个文件夹，且必须包含 SKILL.md）
        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory(System.getProperty("user.dir") + "/src/main/resources/skills")
                .build();

        System.out.println("Registry created with file system");
        return registry;
    }

    /**
     * 示例2：使用 Classpath 加载技能
     * 适用于将技能文件打包在 JAR 包中的场景
     */
    public static SkillRegistry classpathSkillRegistry() {
        // 配置从 classpath:skills 加载
        // 对应资源路径: src/main/resources/skills/
        SkillRegistry registry = ClasspathSkillRegistry.builder()
                .classpathPath("skills")
                .build();

        System.out.println("Registry created with classpath");
        return registry;
    }

    /**
     * 示例3：多级目录配置
     * 用户级 userSkillsDirectory: 全局技能库，所有项目共享
     * 项目级 projectSkillsDirectory: 项目专属技能，仅当前项目使用
     * 当两个目录中存在同名技能时，项目级技能会覆盖用户级技能。
     */
    public static SkillRegistry multiLevelDirectory() {
        // 配置用户级和项目级目录
        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .userSkillsDirectory(System.getProperty("user.home") + "/saa/skills")
                .projectSkillsDirectory(System.getProperty("user.dir") + "/src/main/resources/skills")
                .build();

        System.out.println("Registry created with multi-level directories");
        return registry;
    }

    /**
     * 示例4：自定义系统提示模板
     * 允许用户自定义技能列表如何在 System Prompt 中呈现
     */
    public static SkillRegistry customSystemPromptTemplate() {
        // 自定义模板，包含 {skills_list} 和 {skills_load_instructions} 占位符
        SystemPromptTemplate customTemplate = SystemPromptTemplate.builder()
                .template("## 可用技能\n{skills_list}\n\n## 加载说明\n{skills_load_instructions}")
                .build();

        SkillRegistry registry = ClasspathSkillRegistry.builder()
                .classpathPath("skills")
                .systemPromptTemplate(customTemplate) // 应用自定义模板
                .build();

        System.out.println("Registry configured with custom system prompt template");
        return registry;
    }

    // ==================== 进阶特性 ====================

    /**
     * 示例5：自动重载技能 (Auto Reload)
     * 每次 Agent 执行前重新加载技能，适合调试阶段
     */
    public static SkillsAgentHook autoReloadSkills(SkillRegistry registry) {
        // 开启自动重载
        SkillsAgentHook hook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .autoReload(true) // 每次执行 Agent 前调用 registry.reload()
                .build();

        System.out.println("Hook configured with auto-reload: " + hook.getClass().getSimpleName());
        return hook;
    }

    /**
     * 示例6：渐进式工具披露 (Progressive Tool Disclosure)
     * 只有当模型决定调用 read_skill 读取特定技能后，相关联的工具才会被添加到上下文中。
     * 这可以显著减少 Context Window 的占用，避免干扰模型。
     */
    public static SkillsAgentHook progressiveToolDisclosure(SkillRegistry registry) {
        // 需与 SKILL.md 元数据里的 name 一致
        String skillName = "weekly-report-assistant";

        // 定义一个特定技能才需要的工具，例如 weekly-report-assistant 使用 pythonTool 来运行 skill 里面的 python 脚本
        ToolCallback pythonTool = PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION);

        // 将工具与技能名称绑定 (key 必须与 .md 文件中的 name 字段一致)
        Map<String, List<ToolCallback>> groupedTools = Map.of(
                skillName,
                List.of(pythonTool)
        );

        // 配置带有分组工具的 Hook
        SkillsAgentHook hook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .groupedTools(groupedTools) // 注入工具绑定关系
                .build();

        System.out.println("Hook configured with progressive tool disclosure");
        return hook;
    }

    // ==================== 完整集成示例 ====================

    /**
     * 示例7：完整集成示例
     * 结合 Classpath 技能、自动重载、以及 Shell 工具来完成复杂任务（如生成周报）
     */
    public static void comprehensiveIntegration(SkillsAgentHook skillsHook) throws GraphRunnerException {
        System.out.println("--- Starting Comprehensive Integration Example ---");

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY")) // 建议使用环境变量
                .build();

        // 创建 ChatModel
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 3. Shell Hook：提供 Shell 命令执行，用于读取 markdown 文件
        ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
                .build();

        // 创建 Agent 并注入 Hooks
        ReactAgent agent = ReactAgent.builder()
                .name("skills-agent-full")
                .model(chatModel)
                .saver(new MemorySaver())
                .hooks(List.of(skillsHook, shellHook))
                //.tools(PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION)) // 在 progressiveToolDisclosure 示例中绑定了 python 工具这里就不需要再重复绑定了
                //.enableLogging(true) // 调试时可开启
                .build();

        // 第一轮：询问技能
        System.out.println("User: 请介绍你有哪些技能");
        AssistantMessage response = agent.call("请介绍你有哪些技能");
        System.out.println("Agent: " + response.getText());

        // 第二轮：执行具体任务（需要 writer-skill 或类似技能支持）
        System.out.println("\nUser: 帮我生成周报...");
        String weekReportRequest = """
                帮我生成周报
                
                周一：做了用户模块，大概写了500行代码
                周二：修bug，搞定了支付问题
                周三：开了2次会，讨论需求
                周四：性能优化，提升了不少
                周五：写测试用例 问题：接口文档还没给 下周：继续做订单模块，还要测试
                """;

        response = agent.call(weekReportRequest);
        System.out.println("Agent: " + response.getText());
    }

    public static void main(String[] args) {
        System.out.println("=== Skill Example Tutorial ===");
        System.out.println("注意：确保环境变量 AI_DASHSCOPE_API_KEY 已设置");
        System.out.println("注意：确保 src/main/resources/skills 目录下有相应的技能文件\n");

        try {
            // 示例1：使用文件系统加载技能
            //SkillRegistry registry = fileSystemSkillRegistry();

            // 示例2：使用 Classpath 加载技能
            SkillRegistry registry = classpathSkillRegistry();

            // 示例3：多级目录配置
            //SkillRegistry registry = multiLevelDirectory();

            // 示例4：自定义系统提示模板
            //SkillRegistry registry = customSystemPromptTemplate();

            // 示例5：自动重载技能 (Auto Reload)
            //SkillsAgentHook skillhook = autoReloadSkills(registry);

            // 示例6：渐进式工具披露 (Progressive Tool Disclosure)
            SkillsAgentHook skillhook = progressiveToolDisclosure(registry);

            // 示例7：完整集成示例
            comprehensiveIntegration(skillhook);
        } catch (Exception e) {
            System.err.println("Error executing example: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Note: PythonTool instances created inside progressiveToolDisclosure(...)
            // register a JVM shutdown hook that closes the underlying GraalVM context.
            // In this tutorial main method we do not keep a direct reference to those
            // tools, so there is no explicit close() call here; the shutdown hook
            // performs the required cleanup when the JVM terminates.
            System.out.println("Cleaning up resources (PythonTool will be closed by its shutdown hook)...");
        }
    }

    /**
     * 基于 GraalVM Polyglot 实现的 Python 工具
     */
    public static class PythonTool implements BiFunction<PythonTool.PythonRequest, ToolContext, String>, AutoCloseable {

        public static final String DESCRIPTION = """
            Executes Python code or a local Python script file.
            
            Usage:
            - To run a script file: Provide 'script_path' (absolute path preferred).
            - To run code snippet: Provide 'code'.
            
            The tool will execute the logic and return the result (printed output or return value).
            Security:
                - Python code has IO access (to read/write files) but no access to Java host environment.
                - Only pre-provided scripts in skill folder are allowed to execute.
            """;

        private static final Logger log = LoggerFactory.getLogger(PythonTool.class);
        private final Engine engine;

        public PythonTool() {
            // Create a shared engine for better performance
            this.engine = Engine.newBuilder()
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        }

        /**
         * Create a ToolCallback for the Python tool.
         */
        public static ToolCallback createPythonToolCallback(String description) {
            return FunctionToolCallback.builder("python_tool", new PythonTool())
                    .description(description)
                    .inputType(PythonRequest.class)
                    .build();
        }

        @Override
        public String apply(PythonRequest request, ToolContext toolContext) {
            if ((request.code == null || request.code.isBlank()) &&
                    (request.scriptPath == null || request.scriptPath.isBlank())) {
                return "Error: You must provide either 'code' or 'script_path'.";
            }

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            File scriptFile = null;
            Source source;

            try {
                if (request.scriptPath != null && !request.scriptPath.isBlank()) {
                    scriptFile = getSafeScriptFile(request.scriptPath);
                    log.info("Loading python script from: {}", scriptFile.getAbsolutePath());
                    source = Source.newBuilder("python", scriptFile).build();
                } else {
                    log.debug("Executing inline Python code");
                    source = Source.create("python", request.code);
                }

                Context.Builder contextBuilder = Context.newBuilder("python")
                        .engine(engine)
                        .allowAllAccess(false)
                        .allowIO(true)
                        .out(stdoutStream)
                        .err(stderrStream)
                        .allowHostAccess(HostAccess.NONE);

                if (scriptFile != null) {
                    String scriptDir = scriptFile.getParent();
                    if (scriptDir != null) {
                        contextBuilder.option("python.PythonPath", scriptDir);
                    }
                }

                try (Context context = contextBuilder.build()) {
                    Value result = context.eval(source);
                    // 优先返回 print 的内容
                    String output = stdoutStream.toString(StandardCharsets.UTF_8).trim();
                    String error = stderrStream.toString(StandardCharsets.UTF_8).trim();
                    if (!error.isEmpty()) {
                        return "Script Error:\n" + error;
                    }
                    // 只要脚本里有 print，就返回 print 的内容，而不是 result 对象
                    if (!output.isEmpty()) {
                        return output;
                    }
                    return formatResult(result);
                }
            } catch (IOException e) {
                log.error("IO Error reading script", e);
                return "Error reading script file: " + e.getMessage();
            } catch (PolyglotException e) {
                log.error("Python execution error", e);
                return "Python Runtime Error: " + e.getMessage();
            } catch (Exception e) {
                log.error("Unexpected error", e);
                return "Unexpected Error: " + e.getMessage();
            }
        }

        private String formatResult(Value result) {
            if (result.isNull()) return "Execution completed (No return value)";
            if (result.isString()) return result.asString();
            if (result.isNumber()) return String.valueOf(result.as(Object.class));
            if (result.isBoolean()) return String.valueOf(result.asBoolean());
            if (result.hasArrayElements()) {
                StringBuilder sb = new StringBuilder("[");
                long size = result.getArraySize();
                for (long i = 0; i < size; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(result.getArrayElement(i).toString());
                }
                sb.append("]");
                return sb.toString();
            }
            return result.toString();
        }

        @Override
        public void close() {
            if (this.engine != null) {
                log.info("Closing GraalVM Python Engine...");
                this.engine.close();
            }
        }

        /**
         * Request structure for the Python tool.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PythonRequest {

            @JsonPropertyDescription("The Python code snippet to execute (optional if script_path is provided)")
            public String code;

            @JsonPropertyDescription("The absolute path to a local .py file to execute (optional if code is provided)")
            @JsonProperty("script_path") // 明确映射 JSON 字段名
            public String scriptPath;

            public PythonRequest() {}
        }

        private File getSafeScriptFile(String scriptPath) throws IOException {
            File baseDir = new File("/tmp/skills").getCanonicalFile();
            File targetFile = new File(scriptPath).getCanonicalFile();
            if (!targetFile.getPath().startsWith(baseDir.getPath())) {
                throw new SecurityException("Access denied: Script path is outside the allowed directory.");
            }
            if (!targetFile.exists() || !targetFile.isFile()) {
                throw new FileNotFoundException("Script not found: " + scriptPath);
            }
            return targetFile;
        }
    }
}
