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
package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.DocumentExtractorNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author HeYQ
 * @since 2025-05-02 17:03
 */
@Component
public class DocumentExtractorNodeDataConverter extends AbstractNodeDataConverter<DocumentExtractorNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.DOC_EXTRACTOR.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<DocumentExtractorNodeData>> getDialectConverters() {
		return Stream.of(DocumentExtractorNodeDialectConverter.values())
			.map(DocumentExtractorNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum DocumentExtractorNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public DocumentExtractorNodeData parse(Map<String, Object> data) {
				List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("variable_selector"))
					.filter(CollectionUtils::isNotEmpty)
					.map(variables -> Collections
						.singletonList(new VariableSelector(variables.get(0), variables.get(1))))
					.orElse(Collections.emptyList());
				List<String> fileList = Optional.ofNullable((List<String>) data.get("file_list"))
					.orElse(Collections.emptyList());

				String outputKey = Optional.ofNullable((String) data.get("output_key"))
					.orElse(DocumentExtractorNodeData.DEFAULT_OUTPUT_SCHEMA.getName());

				return new DocumentExtractorNodeData(inputs, List.of(DocumentExtractorNodeData.DEFAULT_OUTPUT_SCHEMA),
						fileList, outputKey);
			}

			@Override
			public Map<String, Object> dump(DocumentExtractorNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				List<VariableSelector> inputs = nodeData.getInputs();
				Optional.ofNullable(inputs)
					.filter(CollectionUtils::isNotEmpty)
					.map(inputList -> inputList.stream()
						.findFirst()
						.map(input -> List.of(input.getNamespace(), input.getName()))
						.orElse(Collections.emptyList()))
					.ifPresent(variables -> data.put("variable_selector", variables));

				List<String> fileList = nodeData.getFileList();
				if (fileList != null && !fileList.isEmpty()) {
					data.put("file_list", fileList);
				}

				String outputKey = nodeData.getOutputKey();
				if (!DocumentExtractorNodeData.DEFAULT_OUTPUT_SCHEMA.getName().equals(outputKey)) {
					data.put("output_key", outputKey);
				}

				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(DocumentExtractorNodeData.class));

		private final DialectConverter<DocumentExtractorNodeData> dialectConverter;

		public DialectConverter<DocumentExtractorNodeData> dialectConverter() {
			return dialectConverter;
		}

		DocumentExtractorNodeDialectConverter(DialectConverter<DocumentExtractorNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
