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
package com.alibaba.cloud.ai.graph.agent.hooks.skills;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class SkillsHookTest {

	private static final String TEST_SKILLS_DIR = "src/test/resources/skills";

	@Test
	void testSkillsHookIntegrationWithScriptExecution(@TempDir Path tempDir) throws Exception {
		System.out.println("\nSkillsHook集成测试\n");

		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();
		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		System.out.println("1. 创建 SkillsHook 并加载 Skills");
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		int skillCount = hook.getSkillCount();
		System.out.println("成功加载 " + skillCount + " 个 Skills");
		assertEquals(3, skillCount, "应该加载 3 个 skills");
		assertTrue(hook.hasSkill("pdf-extractor"), "应该包含 pdf-extractor skill");

		System.out.println("\n2. 分析 Skills 需要的工具...");
		Set<String> requiredTools = hook.getRequiredTools();
		System.out.println("   需要的工具: " + requiredTools);
		assertTrue(requiredTools.contains("shell"), "应该包含 shell 工具");
		assertTrue(requiredTools.contains("read"), "应该包含 read 工具");

		System.out.println("\n3. 创建测试 PDF 文件...");
		Path testPdf = tempDir.resolve("test-report.pdf");
		Files.writeString(testPdf, "Mock PDF content for testing");
		System.out.println("创建测试 PDF: " + testPdf.toAbsolutePath());

		System.out.println("\n4. 准备 Python 脚本...");
		Path skillsDir = tempDir.resolve(".claude").resolve("skills")
			.resolve("pdf-extractor").resolve("scripts");
		Files.createDirectories(skillsDir);
		Path scriptPath = skillsDir.resolve("extract_pdf.py");

		Path sourceScript = Path.of(TEST_SKILLS_DIR, "pdf-extractor", "scripts", "extract_pdf.py");
		Files.copy(sourceScript, scriptPath);
		System.out.println("脚本已复制到: " + scriptPath.toAbsolutePath());


		System.out.println("\n5. 创建 ReactAgent（自动创建工具 + ShellToolAgentHook）...");
		ReactAgent agent = ReactAgent.builder()
			.name("skills-test-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.hooks(
				hook,
				ShellToolAgentHook.builder()
					.shellToolName("shell")  // 匹配工具名称
					.build()
			)
			.tools(hook.createRequiredTools(tempDir.toString()))
			.build();
		System.out.println("ReactAgent 创建成功（包含 ShellToolAgentHook）");

		System.out.println("\n6. 测试 Agent 执行（PDF 提取）...");
		List<Message> messages = List.of(
			new UserMessage("请从 " + testPdf.toAbsolutePath() +
				" 中提取内容。脚本位于 " + scriptPath.toAbsolutePath() + 
				"，使用命令: python " + scriptPath.toAbsolutePath() + " " + testPdf.toAbsolutePath())
		);

		RunnableConfig config = RunnableConfig.builder()
			.threadId("test-integration-thread")
			.build();

		Optional<OverAllState> result = agent.invoke(messages, config);

		// Step 8: Verify results
		System.out.println("\n7. 验证结果...");
		assertTrue(result.isPresent(), "结果应该存在");

		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
		assertNotNull(finalMessages, "消息列表不应为空");
		System.out.println("   返回消息数量: " + finalMessages.size());
		
		long systemMessageCount = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.count();
		System.out.println("   SystemMessage 数量: " + systemMessageCount);
		assertTrue(systemMessageCount <= 2, "SystemMessage 应该很少（理想情况是1条）");

		boolean hasSkillsInfo = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.anyMatch(m -> m.getText().contains("Available Skills") || 
						   m.getText().contains("pdf-extractor"));
		assertTrue(hasSkillsInfo, "应该包含 Skills 信息");
		System.out.println("Skills 信息已注入到上下文");

		boolean hasAssistantResponse = finalMessages.stream()
			.anyMatch(m -> m instanceof AssistantMessage);
		assertTrue(hasAssistantResponse, "应该有 LLM 的回复");
		System.out.println("LLM 成功响应");

		boolean hasToolResponse = finalMessages.stream()
			.anyMatch(m -> m instanceof ToolResponseMessage);
		if (hasToolResponse) {
			System.out.println("工具已被调用");

			boolean hasScriptOutput = finalMessages.stream()
				.filter(m -> m instanceof ToolResponseMessage)
				.anyMatch(m -> m.getText().contains("success") || 
							   m.getText().contains("tables") ||
							   m.getText().contains("metadata"));
			if (hasScriptOutput) {
				System.out.println("脚本执行成功（检测到 JSON 输出）");
			}
		}

		// Print conversation for debugging
		System.out.println("\n8. 对话内容:");
		for (int i = 0; i < finalMessages.size(); i++) {
			Message msg = finalMessages.get(i);
			String msgType = msg.getClass().getSimpleName();
			String preview = msg.getText().substring(0, Math.min(200, msg.getText().length()));
			System.out.println("   [" + i + "] " + msgType + ": " + 
				preview.replace("\n", " ") + (msg.getText().length() > 200 ? "..." : ""));
		}
	}
}
