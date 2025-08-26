package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppModeEnum;
import com.alibaba.cloud.ai.studio.admin.generator.model.agent.Agent;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.GraphProjectDescription;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.ProjectGenerator;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import io.spring.initializr.generator.io.template.TemplateRenderer;
import io.spring.initializr.generator.project.ProjectDescription;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 项目生成器：将 Agent  Schema 转为最小可运行工程（编译 Agent 为 CompiledGraph）
 */
@Component
public class AgentProjectGenerator implements ProjectGenerator {

    private static final String AGENT_BUILDER_TEMPLATE_NAME = "AgentBuilder.java";

    private static final String AGENT_APPLICATION_TEMPLATE_NAME = "AgentApplication.java";

    private static final String PACKAGE_NAME = "packageName";

    private static final String IMPORT_SECTION = "importSection";

    private static final String AGENT_SECTION = "agentSection";

    private static final String HAS_RESOLVER = "hasResolver";

    private final DSLAdapter dslAdapter;

    private final TemplateRenderer templateRenderer;

    public AgentProjectGenerator(@Qualifier("agentDSLAdapter") DSLAdapter dslAdapter,
                                 ObjectProvider<MustacheTemplateRenderer> templateRenderer) {
        this.dslAdapter = dslAdapter;
        this.templateRenderer = templateRenderer.getIfAvailable(() -> new MustacheTemplateRenderer("classpath:/templates"));
    }

    @Override
    public Boolean supportAppMode(AppModeEnum appModeEnum) {
        return Objects.equals(appModeEnum, AppModeEnum.AGENT);
    }

    @Override
    public void generate(GraphProjectDescription projectDescription, Path projectRoot) {
        // 1) 解析 DSL -> Agent 模型
        App app = dslAdapter.importDSL(projectDescription.getDsl());
        Agent root = (Agent) app.getSpec();

        // 2) 渲染构造 Agent 的 Java 代码片段（支持递归/并行）
        RenderContext ctx = new RenderContext();
        String agentSection = renderAgentConstruction(root, ctx);

        // 3) 模板渲染并写入
        Map<String, Object> agentBuilderModel = new HashMap<>();
        agentBuilderModel.put(PACKAGE_NAME, projectDescription.getPackageName());
        agentBuilderModel.put(IMPORT_SECTION, renderImportSection(root));
        agentBuilderModel.put(AGENT_SECTION, agentSection);
        agentBuilderModel.put(HAS_RESOLVER, ctx.hasResolver);

        Map<String, Object> appModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());

        renderAndWriteTemplates(List.of(AGENT_BUILDER_TEMPLATE_NAME, AGENT_APPLICATION_TEMPLATE_NAME),
            List.of(agentBuilderModel, appModel), projectRoot, projectDescription);
    }

    private void renderAndWriteTemplates(List<String> templateNames, List<Map<String, Object>> models, Path projectRoot,
                                         ProjectDescription projectDescription) {
        Path fileRoot = createDirectory(projectRoot, projectDescription);
        for (int i = 0; i < templateNames.size(); i++) {
            String templateName = templateNames.get(i);
            String template;
            try {
                template = templateRenderer.render(templateName, models.get(i));
            }
            catch (IOException e) {
                throw new RuntimeException("Got error when rendering template" + templateName, e);
            }
            Path file;
            try {
                file = Files.createFile(fileRoot.resolve(templateName));
            }
            catch (IOException e) {
                throw new RuntimeException("Got error when creating file", e);
            }
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
                writer.print(template);
            }
            catch (IOException e) {
                throw new RuntimeException("Got error when writing template " + templateName, e);
            }
        }
    }

    private Path createDirectory(Path projectRoot, ProjectDescription projectDescription) {
        StringBuilder pathBuilder = new StringBuilder("src/main/").append(projectDescription.getLanguage().id());
        String packagePath = projectDescription.getPackageName().replace('.', '/');
        pathBuilder.append("/").append(packagePath).append("/agent/");
        try {
            return Files.createDirectories(projectRoot.resolve(pathBuilder.toString()));
        }
        catch (Exception e) {
            throw new RuntimeException("Got error when creating files", e);
        }
    }

    private String renderImportSection(Agent agent) {
        // 预留扩展：按需聚合导入，当前最小实现返回空字符串
        return "";
    }

    // ----------------- code rendering helpers -----------------

    private static class RenderContext {
        boolean hasResolver = false;
        AtomicInteger seq = new AtomicInteger(0);
        String nextVar(String base) { return base + seq.incrementAndGet(); }
    }

    private String renderAgentConstruction(Agent agent, RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        String rootVar = renderAgent(agent, ctx, sb);
        sb.append(String.format("%nreturn %s.getAndCompileGraph();%n", rootVar));
        return sb.toString();
    }

    private String renderAgent(Agent agent, RenderContext ctx, StringBuilder out) {
        String type = optLower(agent.getAgentClass());
        if ("parallel".equals(type)) {
            return renderParallel(agent, ctx, out);
        }
        // default react
        return renderReact(agent, ctx, out);
    }

    private String renderReact(Agent agent, RenderContext ctx, StringBuilder out) {
        String var = ctx.nextVar("reactAgent_");
        String inputKey = orDefault(agent.getInputKey(), "messages");
        out.append(String.format("ReactAgent %s = ReactAgent.builder()%n", var))
            .append(codeIndent(1)).append(String.format(".name(\"%s\")%n", esc(agent.getName())))
            .append(codeIndent(1)).append(String.format(".description(\"%s\")%n", esc(orDefault(agent.getDescription(), ""))))
            .append(codeIndent(1)).append(String.format(".outputKey(\"%s\")%n", esc(orDefault(agent.getOutputKey(), "result"))))
            .append(codeIndent(1)).append(String.format(".llmInputMessagesKey(\"%s\")%n", esc(inputKey)))
            .append(codeIndent(1)).append(".model(chatModel)\n");
        if (notBlank(agent.getInstruction())) {
            out.append(codeIndent(1)).append(String.format(".instruction(\"%s\")%n", esc(agent.getInstruction())));
        }
        if (agent.getMaxIterations() != null && agent.getMaxIterations() > 0) {
            out.append(codeIndent(1)).append(String.format(".maxIterations(%d)%n", agent.getMaxIterations()));
        }
        // resolver 优先于 tools
        if (agent.getToolConfig() != null && agent.getToolConfig().get("resolver") instanceof String) {
            ctx.hasResolver = true;
            out.append(codeIndent(1)).append(".resolver(toolCallbackResolver)\n");
        }
        // 状态策略
        out.append(codeIndent(1)).append(".state(() -> {\n")
            .append(codeIndent(2)).append("Map<String, KeyStrategy> strategies = new HashMap<>();\n")
            .append(codeIndent(2)).append("strategies.put(\"messages\", new AppendStrategy());\n");
        if (agent.getStateConfig() != null) {
            for (Map.Entry<String, String> e : agent.getStateConfig().entrySet()) {
                String key = e.getKey();
                String strat = optLower(e.getValue());
                if ("messages".equals(key)) continue; // 已默认
                if ("replace".equals(strat)) {
                    out.append(codeIndent(2)).append(String.format("strategies.put(\"%s\", new ReplaceStrategy());\n", esc(key)));
                } else {
                    out.append(codeIndent(2)).append(String.format("strategies.put(\"%s\", new AppendStrategy());\n", esc(key)));
                }
            }
        }
        out.append(codeIndent(2)).append("return strategies;\n")
            .append(codeIndent(1)).append("})\n")
            .append(codeIndent(1)).append(".build();\n");
        return var;
    }

    private String renderParallel(Agent agent, RenderContext ctx, StringBuilder out) {
        // 先构建所有子 agent
        List<String> childVars = new ArrayList<>();
        if (agent.getSubAgents() != null) {
            for (Agent sub : agent.getSubAgents()) {
                childVars.add(renderAgent(sub, ctx, out));
            }
        }

        String var = ctx.nextVar("parallelAgent_");
        out.append(String.format("ParallelAgent %s = ParallelAgent.builder()%n", var))
            .append(codeIndent(1)).append(String.format(".name(\"%s\")%n", esc(agent.getName())))
            .append(codeIndent(1)).append(String.format(".description(\"%s\")%n", esc(orDefault(agent.getDescription(), ""))))
            .append(codeIndent(1)).append(String.format(".outputKey(\"%s\")%n", esc(orDefault(agent.getOutputKey(), "result"))));
        if (notBlank(agent.getInputKey())) {
            out.append(codeIndent(1)).append(String.format(".inputKey(\"%s\")%n", esc(agent.getInputKey())));
        }
        if (!childVars.isEmpty()) {
            out.append(codeIndent(1)).append(".subAgents(List.of(")
                .append(String.join(", ", childVars))
                .append("))\n");
        }
        // merge strategy
        Map<String, Object> flow = agent.getFlowConfig();
        if (flow != null) {
            String strategy = optLower(String.valueOf(flow.get("merge_strategy")));
            if ("list".equals(strategy)) {
                out.append(codeIndent(1)).append(".mergeStrategy(new ParallelAgent.ListMergeStrategy())\n");
            } else if ("concat".equals(strategy)) {
                String sep = String.valueOf(flow.getOrDefault("separator", "\\n"));
                out.append(codeIndent(1)).append(String.format(".mergeStrategy(new ParallelAgent.ConcatenationMergeStrategy(\"%s\"))%n", esc(sep)));
            } else if ("default".equals(strategy) || strategy != null) {
                out.append(codeIndent(1)).append(".mergeStrategy(new ParallelAgent.DefaultMergeStrategy())\n");
            }
            Object mc = flow.get("max_concurrency");
            if (mc instanceof Number && ((Number) mc).intValue() > 0) {
                out.append(codeIndent(1)).append(String.format(".maxConcurrency(%d)%n", ((Number) mc).intValue()));
            }
        }
        // 状态策略
        out.append(codeIndent(1)).append(".state(() -> {\n")
            .append(codeIndent(2)).append("Map<String, KeyStrategy> strategies = new HashMap<>();\n")
            .append(codeIndent(2)).append("strategies.put(\"messages\", new AppendStrategy());\n");
        if (agent.getStateConfig() != null) {
            for (Map.Entry<String, String> e : agent.getStateConfig().entrySet()) {
                String key = e.getKey();
                String strat = optLower(e.getValue());
                if ("messages".equals(key)) continue;
                if ("replace".equals(strat)) {
                    out.append(codeIndent(2)).append(String.format("strategies.put(\"%s\", new ReplaceStrategy());\n", esc(key)));
                } else {
                    out.append(codeIndent(2)).append(String.format("strategies.put(\"%s\", new AppendStrategy());\n", esc(key)));
                }
            }
        }
        out.append(codeIndent(2)).append("return strategies;\n")
            .append(codeIndent(1)).append("})\n")
            .append(codeIndent(1)).append(".build();\n");
        return var;
    }

    private String codeIndent(int level) {
        return "\t".repeat(Math.max(0, level));
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String orDefault(String v, String d) {
        return v == null || v.isEmpty() ? d : v;
    }

    private String optLower(String v) {
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }
}
