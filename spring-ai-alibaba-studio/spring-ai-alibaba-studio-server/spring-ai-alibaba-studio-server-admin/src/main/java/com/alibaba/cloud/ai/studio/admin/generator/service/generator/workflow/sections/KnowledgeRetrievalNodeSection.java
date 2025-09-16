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
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.KnowledgeRetrievalNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.rag.DocumentService;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentQuery;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// TODO: 支持其他格式的文档，如PDF、ZIP等
// TODO: 解析并应用RerankModel、EmbeddingModel配置
// TODO: 支持从OSS获取资源文件，或者在生成项目中从OSS获取资源文件
@Component
public class KnowledgeRetrievalNodeSection implements NodeSection<KnowledgeRetrievalNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.RETRIEVER.equals(nodeType);
	}

	// 用于获取Studio存储的文档
	private final DocumentService studioDocumentService;

	private final String studioStoragePath;

	public KnowledgeRetrievalNodeSection(@Autowired(required = false) DocumentService studioDocumentService,
			@Autowired(required = false) StudioProperties properties) {
		this.studioDocumentService = studioDocumentService;
		this.studioStoragePath = properties != null ? properties.getStoragePath() : null;
	}

	@Override
	public String render(Node node, String varName) {
		KnowledgeRetrievalNodeData nodeData = (KnowledgeRetrievalNodeData) node.getData();

		if (DSLDialectType.STUDIO.equals(nodeData.getDialectType())) {
			if (this.studioDocumentService == null || this.studioStoragePath == null) {
				throw new IllegalArgumentException(
						"The current mode does not support Studio's knowledge retrieval node code generation. Please start the complete StudioApplication class");
			}
			// 根据knowledgeBaseIds获取对应的资源文件
			List<ResourceFile> resourceFiles = Optional.ofNullable(nodeData.getKnowledgeBaseIds())
				.orElse(List.of())
				.stream()
				.map(kbId -> {
					PagingList<Document> getSize = this.studioDocumentService.listDocuments(kbId, new DocumentQuery());
					Long total = getSize.getTotal();
					DocumentQuery query = new DocumentQuery();
					query.setSize(total.intValue());
					PagingList<Document> pagingList = this.studioDocumentService.listDocuments(kbId, query);
					return pagingList.getRecords();
				})
				.flatMap(List::stream)
				.filter(Document::getEnabled)
				.filter(d -> StringUtils.hasText(d.getPath()))
				.map(document -> {
					// 文件类型
					String contentType = document.getMetadata().getContentType();
					// 存储形式
					DocumentType documentType = document.getType();
					// 存储路径
					String path = switch (documentType) {
						case FILE -> {
							{
								Path p = Path.of(studioStoragePath);
								Path resolvedPath = p.resolve(document.getPath()).normalize();

								// 安全检查：确保解析后的路径仍在允许的目录范围内
								if (!resolvedPath.startsWith(p.normalize())) {
									throw new SecurityException("非法路径访问尝试: " + document.getPath());
								}

								yield resolvedPath.toAbsolutePath().toString();
							}
						}
						case URL -> {
							// 对URL路径进行基本验证
							String urlPath = document.getPath();
							if (urlPath == null || urlPath.trim().isEmpty()) {
								throw new IllegalArgumentException("URL路径不能为空");
							}
							yield urlPath;
						}
						default ->
							throw new UnsupportedOperationException("unsupported document type: " + documentType);
					};
					String fileName = document.getName();
					// 构造文件记录
					return new ResourceFile(fileName, switch (documentType) {
						case FILE -> ResourceFile.Type.CLASS_PATH;
						case URL -> ResourceFile.Type.URL;
						default ->
							throw new UnsupportedOperationException("unsupported document type: " + documentType);
					}, () -> {
						try {
							return Files.newInputStream(Path.of(path));
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				})
				.toList();
			nodeData.setResourceFiles(resourceFiles);
		}

		return String.format("""
				// —— KnowledgeRetrievalNode [%s] ——
				KnowledgeRetrievalNode %s = KnowledgeRetrievalNode.builder()
				    .topK(%s)
				    .similarityThreshold(%s)
				    .inputKey(%s)
				    .outputKey(%s)
				    .vectorStore(createVectorStore(%s))
				    .build();
				stateGraph.addNode("%s", AsyncNodeAction.node_async(wrapperRetrievalNodeAction(%s, "%s")));

				""", node.getId(), varName, ObjectToCodeUtil.toCode(nodeData.getTopK()),
				ObjectToCodeUtil.toCode(nodeData.getThreshold()), ObjectToCodeUtil.toCode(nodeData.getInputKey()),
				ObjectToCodeUtil.toCode(nodeData.getOutputKey()),
				ObjectToCodeUtil.toCode(DSLDialectType.STUDIO.equals(nodeData.getDialectType())
						? nodeData.getResourceFiles() : List.of("please_config_your_own_resource_files")),
				varName, varName, nodeData.getOutputKey());
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		StringBuilder sb = new StringBuilder();
		sb.append("""
				@Autowired
				private ResourceLoader resourceLoader;

				@Autowired
				private EmbeddingModel embeddingModel;

				""");
		if (!DSLDialectType.STUDIO.equals(dialectType)) {
			sb.append(
					"// todo: Please manually modify the parameter values passed to this method to point to the correct resource paths");
			sb.append(String.format("%n"));
		}
		sb.append("""
				private VectorStore createVectorStore(List<String> paths) {
				    List<Resource> resources = Optional.ofNullable(paths).orElse(List.of())
				            .stream().map(resourceLoader::getResource).toList();
				    List<Document> documents = resources.stream().map(TextReader::new).map(TextReader::read)
				            .flatMap(List::stream).toList();
				    List<Document> chunks = new TokenTextSplitter().transform(documents);
				    SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
				    vectorStore.write(chunks);
				    return vectorStore;
				}
				""");
		sb.append(switch (dialectType) {
			case DIFY ->
				"""
						 private NodeAction wrapperRetrievalNodeAction(NodeAction nodeAction, String key) {
						     return state -> {
						         // Convert the result to the variable format required by the Dify workflow
						         Map<String, Object> result = nodeAction.apply(state);
						         Object object = result.get(key);
						         if(object instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Document) {
						             // Return value is Array[Object] (using List<Map>)
						             List<Document> documentList = (List<Document>) list;
						             List<Map<String, Object>> mapList = documentList.stream().map(document ->
						                             Map.of("content", document.getFormattedContent(), "title", document.getId(), "url", "", "icon", "", "metadata", document.getMetadata()))
						                     .toList();
						             return Map.of(key, mapList);
						         } else {
						             return Map.of(key, List.of(Map.of("content", object.toString(), "title", "unknown", "url", "unknown", "icon", "unknown", "metadata", object)));
						         }
						     };
						 }
						""";
			case STUDIO ->
				"""
						   private NodeAction wrapperRetrievalNodeAction(NodeAction nodeAction, String key) {
						       return state -> {
						           // Convert the result to the variable format required by the workflow
						           Map<String, Object> result = nodeAction.apply(state);
						           Object object = result.get(key);
						           if (object instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Document) {
						               // Return value is Array[Object] (using List<Map>)
						               List<Document> documentList = (List<Document>) list;
						               List<Map<String, Object>> mapList = documentList.stream()
						                   .map(document -> Map.<String, Object>of("doc_id", document.getId(), "doc_name",
						                           Optional.ofNullable(document.getText()).orElse("unknown"), "title", document.getId(),
						                           "text", document.getFormattedContent(), "score",
						                           Optional.ofNullable(document.getScore()).orElse(0.0), "page_number", 0, "chunk_id",
						                           document.getId()))
						                   .toList();
						               return Map.of(key, mapList);
						           }
						           else {
						               return Map.of(key, List.of());
						           }
						       };
						   }
						""";
			default -> "";
		});
		return sb.toString();
	}

	@Override
	public List<ResourceFile> resourceFiles(DSLDialectType dialectType, KnowledgeRetrievalNodeData nodeData) {
		return Optional.ofNullable(nodeData.getResourceFiles())
			.orElse(NodeSection.super.resourceFiles(dialectType, nodeData));
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.KnowledgeRetrievalNode",
				"org.springframework.ai.embedding.EmbeddingModel", "org.springframework.ai.reader.TextReader",
				"org.springframework.ai.transformer.splitter.TokenTextSplitter",
				"org.springframework.ai.vectorstore.SimpleVectorStore",
				"org.springframework.ai.vectorstore.VectorStore", "org.springframework.beans.factory.annotation.Value",
				"org.springframework.core.io.Resource", "org.springframework.ai.document.Document",
				"org.springframework.beans.factory.annotation.Autowired", "org.springframework.core.io.ResourceLoader",
				"java.util.Optional");
	}

}
