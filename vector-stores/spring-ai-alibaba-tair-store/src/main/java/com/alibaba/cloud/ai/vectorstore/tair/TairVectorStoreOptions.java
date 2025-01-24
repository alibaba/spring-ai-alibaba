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
package com.alibaba.cloud.ai.vectorstore.tair;

import com.aliyun.tair.tairvector.params.DistanceMethod;
import com.aliyun.tair.tairvector.params.IndexAlgorithm;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration options for the Tair Vector Store.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class TairVectorStoreOptions {

	/**
	 * Default index parameters for the vector store.
	 */
	public static final List<String> DEFAULT_INDEX_PARAMS = Arrays.asList("ef_construct", "100", "M", "16");

	/**
	 * Default index name for the vector store.
	 */
	public static final String DEFAULT_INDEX_NAME = "spring_ai_tair_vector_store";

	/**
	 * The name of the index in the vector store.
	 */
	private String indexName = DEFAULT_INDEX_NAME;

	/**
	 * The dimension of the vectors to be stored in the index. All vectors inserted into
	 * this index must have the same dimension. Valid range is [1, 32768].
	 */
	private int dimensions = 1536;

	/**
	 * The algorithm used for building and querying the index. FLAT: No separate index is
	 * built; brute-force search is used for queries, suitable for small datasets (up to
	 * 10,000 entries). HNSW: An HNSW graph structure is used to build the index and
	 * perform queries, suitable for large datasets.
	 */
	private IndexAlgorithm indexAlgorithm = IndexAlgorithm.HNSW;

	/**
	 * The method used to calculate the distance between vectors. L2: Euclidean distance
	 * squared. IP: Inner product. JACCARD: Jaccard distance, requires specifying the
	 * vector data type (data_type) as BINARY.
	 */
	private DistanceMethod distanceMethod = DistanceMethod.L2;

	/**
	 * Parameters for the index. Refer to the official documentation for more details:
	 * <a href="https://help.aliyun.com/zh/tair">Tair Documentation</a>
	 */
	private List<String> indexParams = DEFAULT_INDEX_PARAMS;

	/**
	 * 过期时间，单位为秒。
	 */
	private Integer expireSeconds = 600;

	/**
	 * The expiration time for the index, in seconds.
	 */
	public String getIndexName() {
		return indexName;
	}

	/**
	 * Sets the name of the index.
	 * @param indexName the index name to set
	 */
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	/**
	 * Returns the dimension of the vectors.
	 * @return the vector dimension
	 */
	public int getDimensions() {
		return dimensions;
	}

	/**
	 * Sets the dimension of the vectors.
	 * @param dimensions the vector dimension to set
	 */
	public void setDimensions(int dimensions) {
		this.dimensions = dimensions;
	}

	/**
	 * Returns the index algorithm.
	 * @return the index algorithm
	 */
	public IndexAlgorithm getIndexAlgorithm() {
		return indexAlgorithm;
	}

	/**
	 * Sets the index algorithm.
	 * @param indexAlgorithm the index algorithm to set
	 */
	public void setIndexAlgorithm(IndexAlgorithm indexAlgorithm) {
		this.indexAlgorithm = indexAlgorithm;
	}

	/**
	 * Returns the distance method.
	 * @return the distance method
	 */
	public DistanceMethod getDistanceMethod() {
		return distanceMethod;
	}

	/**
	 * Sets the distance method.
	 * @param distanceMethod the distance method to set
	 */
	public void setDistanceMethod(DistanceMethod distanceMethod) {
		this.distanceMethod = distanceMethod;
	}

	/**
	 * Returns the index parameters.
	 * @return the index parameters
	 */
	public List<String> getIndexParams() {
		return indexParams;
	}

	/**
	 * Sets the index parameters.
	 * @param indexParams the index parameters to set
	 */
	public void setIndexParams(List<String> indexParams) {
		this.indexParams = indexParams;
	}

	/**
	 * Returns the expiration time for the index.
	 * @return the expiration time in seconds
	 */
	public Integer getExpireSeconds() {
		return expireSeconds;
	}

	/**
	 * Sets the expiration time for the index.
	 * @param expireSeconds the expiration time in seconds to set
	 */
	public void setExpireSeconds(Integer expireSeconds) {
		this.expireSeconds = expireSeconds;
	}

}