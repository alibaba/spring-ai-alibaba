package com.alibaba.cloud.ai.dashscope.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ANALYTICDB_SECRET_KEY", matches = ".+")
class AnalyticdbVectorTest {

	AnalyticdbVector analyticdbVector;

	@BeforeEach
	public void init() throws Exception {
		AnalyticdbConfig config = new AnalyticdbConfig();
		config.setRegionId("cn-beijing");
		config.setDBInstanceId("gp-2ze41j8y0ry4spfev");
		config.setAccessKeyId(System.getenv("ANALYTICDB_SECRET_ID"));
		config.setAccessKeySecret(System.getenv("ANALYTICDB_SECRET_KEY"));
		config.setManagerAccount("hyq"); // admin0
		config.setManagerAccountPassword("hdcHDC1997@@@"); // 123456
		config.setNamespace("llama");
		config.setNamespacePassword("llamapassword");
		config.setEmbeddingDimension(3L);
		analyticdbVector = new AnalyticdbVector("test_llama", config);
	}

	@Test
	void testGetInstance() throws Exception {
		List<Document> list = new ArrayList<>(10);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("docId", "1"); // 123 //12344
		Document document = new Document("你好吗1234你是women12334444", metadata);
		int length = 1536; // 数组长度
		float min = 0f; // 最小值
		float max = 1f; // 最大值
		float[] em = new float[length]; // 创建 float 数组
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			em[i] = min + (max - min) * random.nextFloat();
		}
		document.setEmbedding(em);
		list.add(document);
		analyticdbVector.add(list);
		SearchRequest searchRequest = SearchRequest.query("你好");
		List<Document> documents = analyticdbVector.similaritySearch(searchRequest);
		System.out.println(documents.get(0).getContent());

		// analyticdbVector.delete(List.of("1"));

	}

	@Test
	void testSearchByVector() {
		// 假设我们有一个已知的向量和一些预设的参数
		// List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f);
		// Map<String, Object> kwargs = new HashMap<>();
		// kwargs.put("score_threshold", 0.5f);
		SearchRequest searchRequest = SearchRequest.query("你好");
		searchRequest.withTopK(5);
		searchRequest.withSimilarityThreshold(0.5f);

		// 调用方法并验证返回结果
		List<Document> results = analyticdbVector.similaritySearch(searchRequest);

		// 这里应该有一些断言来验证结果是否符合预期
		Assertions.assertNotNull(results);
		// 更具体的断言可以根据你的需求添加
	}

	@Test
	void testDelete() {
		// 调用 delete 方法
		analyticdbVector.delete(List.of("1"));

		// 根据你的实际情况，这里可以添加验证删除操作是否成功的逻辑
		// 例如，检查数据库中是否存在该集合
	}

}
