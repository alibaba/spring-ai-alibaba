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
package com.alibaba.cloud.ai.vectorstore.tablestore.example;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.search.FieldSchema;
import com.alicloud.openservices.tablestore.model.search.FieldType;
import com.alicloud.openservices.tablestore.model.search.vector.VectorMetricType;
import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeStoreImpl;

import java.util.Arrays;
import java.util.List;

public class KnowledgeStoreInitExample {

	public void example() {
		/*
		 * 初始化TableStore客户端
		 */
		String endPoint = "your endPoint";
		String instanceName = "your instanceName";
		String accessKeyId = "your accessKeyId";
		String accessKeySecret = "your accessKeySecret";
		SyncClient client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);

		/*
		 * 初始化 KnowledgeStore
		 */

		// 定义哪些额外的meta字段定义到多元索引中，这样使用多元索引可以搜索这些字段。
		List<FieldSchema> extraMetaDataIndexSchema = Arrays.asList(
				new FieldSchema("meta_example_string", FieldType.KEYWORD),
				new FieldSchema("meta_example_text_1", FieldType.TEXT).setAnalyzer(FieldSchema.Analyzer.MaxWord),
				new FieldSchema("meta_example_text_2", FieldType.TEXT).setAnalyzer(FieldSchema.Analyzer.SingleWord),
				new FieldSchema("meta_example_long", FieldType.LONG),
				new FieldSchema("meta_example_double", FieldType.DOUBLE),
				new FieldSchema("meta_example_boolean", FieldType.BOOLEAN));

		/*
		 * 这里介绍下多租户，租户可以是子知识库、用户、组织等，具体可以参考业务场景。通常来说使用用户id或者知识库id当做租户id有通用性。
		 *
		 * <li>如果业务是多租户场景，那么开启多租户优化有助于提高性能，增加召回率。</li>
		 *
		 * <li>如果业务是单租户场景，忽略该参数即可。通常单租户场景，每次查询会查询整个库里的所有数据，而不是某一个用户/子知识库/子模块的部分数据。</li>
		 *
		 * 因多租户场景较为常见，这里以开启多租户为例。
		 */
		boolean enableMultiTenant = true;

		// 如需自定义其它参数，可以自己通过builder选择自己需要的
		KnowledgeStoreImpl store = KnowledgeStoreImpl.builder()
			.client(client)
			.metadataSchema(extraMetaDataIndexSchema)
			.embeddingDimension(512) // 向量维度必须设置。
										// 通常情况下选择Embedding模型的维度在512~1024之间。太高维度会增加检索时间，带来的边际收益会下降。
			.embeddingMetricType(VectorMetricType.DOT_PRODUCT) // 向量检索评分公式，这里以内积为例。
			.enableMultiTenant(enableMultiTenant) // 该参数必须设置
			.build();

		// 初始化表(表和多元索引会自动创建)
		store.initTable();

		// 删除表和索引(方便测试期间使用)
		store.deleteTableAndIndex();
	}

}
