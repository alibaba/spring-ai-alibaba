/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: 支持异常分支
@Component
public class CodeNodeSection implements NodeSection<CodeNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.CODE.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		CodeNodeData nodeData = ((CodeNodeData) node.getData());
		return String.format("""
				// —— CodeNode [%s] ——
				CodeExecutorNodeAction %s = CodeExecutorNodeAction.builder()
				        .codeExecutor(codeExecutor)
				        .codeLanguage("%s")
				        .code(%s)
				        .codeStyle(%s)
				        .config(codeExecutionConfig)
				        .params(%s)
				        .outputKey("%s")
				        .build();
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    wrapperCodeNodeAction(%s, "%s", "%s", %d, %d, %s)
				));

				""", node.getId(), varName, nodeData.getCodeLanguage(), ObjectToCodeUtil.toCode(nodeData.getCode()),
				ObjectToCodeUtil.toCode(nodeData.getCodeStyle()), ObjectToCodeUtil.toCode(nodeData.getInputParams()),
				nodeData.getOutputKey(), varName, varName, nodeData.getOutputKey(), varName,
				nodeData.getMaxRetryCount(), nodeData.getRetryIntervalMs(),
				ObjectToCodeUtil.toCode(nodeData.getDefaultValue()));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO ->
				"""
						   private static final CodeExecutionConfig codeExecutionConfig;
						   private static final CodeExecutor codeExecutor;

						   static {
						       // todo: configure your own code execution configuration
						       try {
						           Path tempDir = Files.createTempDirectory("code-execution-workdir-");
						           tempDir.toFile().deleteOnExit();
						           codeExecutionConfig = new CodeExecutionConfig().setWorkDir(tempDir.toString());
						           codeExecutor = new LocalCommandlineCodeExecutor();
						       } catch (Exception e) {
						           throw new RuntimeException(e);
						       }
						   }

						   private NodeAction wrapperCodeNodeAction(NodeAction codeNodeAction, String key,
						                    String nodeName, int maxRetryCount, int retryIntervalMs, Map<String, Object> defaultValue) {
						       return state -> {
						           int count = maxRetryCount;
						           while (count-- > 0) {
						               try {
						                   // 将代码运行的结果拆包
						                   Map<String, Object> result = codeNodeAction.apply(state);
						                   Object object = result.get(key);
						                   if(!(object instanceof Map)) {
						                       throw new RuntimeException("unexcepted result");
						                   }
						                   return ((Map<String, Object>) object).entrySet().stream()
						                           .collect(Collectors.toMap(
						                                   entry -> nodeName + "_" + entry.getKey(),
						                                   Map.Entry::getValue
						                           ));
						               } catch (Exception e) {
						                   Thread.sleep(retryIntervalMs);
						               }
						           }
						           if(defaultValue != null) {
						               return defaultValue;
						           } else {
						               throw new RuntimeException("code execution failed!");
						           }
						       };
						   }
						""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction",
				"com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig",
				"com.alibaba.cloud.ai.graph.node.code.CodeExecutor",
				"com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor", "java.io.IOException",
				"java.nio.file.Files", "java.nio.file.Path", "java.util.stream.Collectors",
				"com.alibaba.cloud.ai.graph.node.code.entity.CodeParam",
				"com.alibaba.cloud.ai.graph.node.code.entity.CodeStyle");
	}

}
