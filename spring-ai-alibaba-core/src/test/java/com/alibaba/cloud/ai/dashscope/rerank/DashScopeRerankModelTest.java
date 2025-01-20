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

package com.alibaba.cloud.ai.dashscope.rerank;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Title DashScope rerank model test cases.<br/>
 * Description DashScope rerank model test cases.<br/>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_HTTP_BASE_URL", matches = ".+")
public class DashScopeRerankModelTest {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeRerankModelTest.class);

	@Autowired
	private RerankModel dashscopeRerankModel;

	@Test
	void testRerank() {
		String query = "什么是文本排序模型";
		List<Document> documents = new ArrayList<>();
		documents.add(Document.builder().text("文本排序模型广泛用于搜索引擎和推荐系统中，它们根据文本相关性对候选文本进行排序").build());
		documents.add(Document.builder().text("量子计算是计算科学的一个前沿领域").build());
		documents.add(Document.builder().text("预训练语言模型的发展给文本排序模型带来了新的进展").build());
		documents.add(Document.builder().text("文本排序模型能够帮助检索增强生成提升效果").build());

		RerankRequest request = new RerankRequest(query, documents);
		RerankResponse response = dashscopeRerankModel.call(request);
		Assertions.assertNotNull(response);

		for (int i = 0; i < response.getResults().size(); i++) {
			DocumentWithScore document = response.getResults().get(i);
			logger.info("content: {}, score: {}", document.getOutput().getContent(), document.getScore());
		}

		logger.info("usage: {}", response.getMetadata().getUsage().getTotalTokens());
	}

}