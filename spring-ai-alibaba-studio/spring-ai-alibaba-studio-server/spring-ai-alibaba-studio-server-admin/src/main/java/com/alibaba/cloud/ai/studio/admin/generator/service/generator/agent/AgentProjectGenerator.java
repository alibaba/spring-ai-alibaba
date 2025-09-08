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

/**
 * Agent 项目生成器：将 Agent  Schema 转为最小可运行工程（编译 Agent 为 CompiledGraph）
 */
@Component
public class AgentProjectGenerator implements ProjectGenerator {

    private static final String AGENT_BUILDER_TEMPLATE_NAME = "AgentBuilder.java";

    private static final String GRAPH_RUN_TEMPLATE_NAME = "GraphRunController.java";

    private static final String PACKAGE_NAME = "packageName";

    private static final String IMPORT_SECTION = "importSection";

    private static final String AGENT_SECTION = "agentSection";

    private static final String HAS_RESOLVER = "hasResolver";

    private final DSLAdapter dslAdapter;

    private final TemplateRenderer templateRenderer;
    private final AgentTypeProviderRegistry providerRegistry;

    public AgentProjectGenerator(@Qualifier("agentDSLAdapter") DSLAdapter dslAdapter,
                                 ObjectProvider<MustacheTemplateRenderer> templateRenderer,
                                 AgentTypeProviderRegistry providerRegistry) {
        this.dslAdapter = dslAdapter;
        this.templateRenderer = templateRenderer.getIfAvailable(() -> new MustacheTemplateRenderer("classpath:/templates"));
        this.providerRegistry = providerRegistry;
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
        CodeSections sections = collectSections(root, ctx);
        String agentSection = sections.getCode() + String.format("%nreturn %s.getAndCompileGraph();%n", sections.getVarName());

        // 3) 模板渲染并写入
        Map<String, Object> agentBuilderModel = new HashMap<>();
        agentBuilderModel.put(PACKAGE_NAME, projectDescription.getPackageName());
        agentBuilderModel.put(IMPORT_SECTION, String.join("\n", sections.getImports()));
        agentBuilderModel.put(AGENT_SECTION, agentSection);
        agentBuilderModel.put(HAS_RESOLVER, sections.isHasResolver());

        Map<String, Object> graphRunModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());

        renderAndWriteTemplates(List.of(AGENT_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
            List.of(agentBuilderModel, graphRunModel), projectRoot, projectDescription);
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
        pathBuilder.append("/").append(packagePath).append("/graph/");
        try {
            return Files.createDirectories(projectRoot.resolve(pathBuilder.toString()));
        }
        catch (Exception e) {
            throw new RuntimeException("Got error when creating files", e);
        }
    }

    private CodeSections collectSections(Agent agent, RenderContext ctx) {
        // 递归先处理子 agent，收集子 varNames
        List<String> childVars = new ArrayList<>();
        List<CodeSections> childSections = new ArrayList<>();
        if (agent.getSubAgents() != null) {
            for (Agent sub : agent.getSubAgents()) {
                CodeSections cs = collectSections(sub, ctx);
                childSections.add(cs);
                childVars.add(cs.getVarName());
            }
        }

        // 当前节点由 Provider 渲染
        AgentTypeProvider provider = providerRegistry.get(agent.getAgentClass());
        AgentShell shell = AgentShell.of(
            agent.getAgentClass(),
            agent.getName(),
            agent.getDescription(),
            agent.getInstruction(),
            agent.getInputKeys(),
            agent.getOutputKey()
        );
        Map<String,Object> handle = agent.getHandle() == null ? java.util.Map.of() : agent.getHandle();
        CodeSections me = provider.render(shell, handle, ctx, childVars);

        // 合并 imports 与 hasResolver，拼接顺序：子在前、父在后
        for (CodeSections cs : childSections) {
            me.getImports().addAll(cs.getImports());
            if (cs.isHasResolver()) me.resolver(true);
        }
        StringBuilder code = new StringBuilder();
        for (CodeSections cs : childSections) code.append(cs.getCode());
        code.append(me.getCode());
        me.code(code.toString());
        return me;
    }
}
