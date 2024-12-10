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
		Document document = new Document("hello1234you arewomen12334444", metadata);
		int length = 1536; // Array length
		float min = 0f; // smallest value
		float max = 1f; // the largest value
		float[] em = new float[length]; // create float array
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			em[i] = min + (max - min) * random.nextFloat();
		}
		document.setEmbedding(em);
		list.add(document);
		analyticdbVector.add(list);
		SearchRequest searchRequest = SearchRequest.query("hello");
		List<Document> documents = analyticdbVector.similaritySearch(searchRequest);
		System.out.println(documents.get(0).getContent());

		// analyticdbVector.delete(List.of("1"));

	}

	@Test
	void testSearchByVector() {
		// Suppose we have a known vector and some preset parameters.
		// List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f);
		// Map<String, Object> kwargs = new HashMap<>();
		// kwargs.put("score_threshold", 0.5f);
		SearchRequest searchRequest = SearchRequest.query("hello");
		searchRequest.withTopK(5);
		searchRequest.withSimilarityThreshold(0.5f);

		// Call the method and verify the return result.
		List<Document> results = analyticdbVector.similaritySearch(searchRequest);

		// There should be some assertions here to verify that the results meet
		// expectations.
		Assertions.assertNotNull(results);
		// The more specific assertions can be added based on your needs.
	}

	@Test
	void testDelete() {
		// Call the delete method.
		analyticdbVector.delete(List.of("1"));

		// Based on your actual situation, you can add logic here to verify
		// whether the delete operation was successful.
		// For example, check whether the collection exists in the database.
	}

}
