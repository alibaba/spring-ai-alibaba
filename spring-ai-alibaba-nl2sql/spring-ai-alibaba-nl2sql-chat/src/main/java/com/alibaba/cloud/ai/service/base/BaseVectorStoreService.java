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
package com.alibaba.cloud.ai.service.base;

import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class BaseVectorStoreService {

	/**
	 * 获取嵌入模型
	 */
	protected abstract EmbeddingModel getEmbeddingModel();

	/**
	 * 将文本转换为 Double 类型的向量
	 */
	public List<Double> embedDouble(String text) {
		return convertToDoubleList(getEmbeddingModel().embed(text));
	}

	public Boolean schema(SchemaInitRequest schemaInitRequest) throws Exception {
		throw new UnsupportedOperationException("Not implemented.");
	}

	/**
	 * 将文本转换为 Float 类型的向量
	 */
	public List<Float> embedFloat(String text) {
		return convertToFloatList(getEmbeddingModel().embed(text));
	}

	/**
	 * 获取向量库中的文档
	 */
	public List<Document> getDocuments(String query, String vectorType) {
		SearchRequest request = new SearchRequest();
		request.setQuery(query);
		request.setVectorType(vectorType);
		request.setTopK(100);
		return new ArrayList<>(searchWithVectorType(request));
	}

	/**
	 * 默认 filter 的搜索接口
	 */
	public abstract List<Document> searchWithVectorType(SearchRequest searchRequestDTO);

	/**
	 * 自定义 filter 的搜索接口
	 */
	public abstract List<Document> searchWithFilter(SearchRequest searchRequestDTO);

	/**
	 * 获取表的文档
	 */
	public List<Document> searchTableByNameAndVectorType(SearchRequest searchRequestDTO) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	/**
	 * 将 float[] 转换为 Double List
	 */
	protected List<Double> convertToDoubleList(float[] array) {
		return IntStream.range(0, array.length)
			.mapToDouble(i -> (double) array[i])
			.boxed()
			.collect(Collectors.toList());
	}

	/**
	 * 将 float[] 转换为 Float List
	 */
	protected List<Float> convertToFloatList(float[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}

}
