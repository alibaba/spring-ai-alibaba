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
package com.alibaba.cloud.ai.reader.tencent.cos;

import com.alibaba.cloud.ai.document.TextDocumentParser;
import com.alibaba.cloud.ai.document.DocumentParser;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HeYQ
 * @author brianxiadong
 * @since 2024-11-27 21:41
 */
@EnabledIfEnvironmentVariable(named = "TENCENT_SECRET_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TENCENT_SECRET_ID", matches = ".+")
class TencentCosDocumentLoaderIT {

	private static final String TEST_BUCKET = "test-buket";

	private static final String TEST_KEY = "test-file.txt";

	private static final String TEST_KEY_2 = "test-directory/test-file-2.txt";

	private static final String TEST_CONTENT = "Hello, World!";

	private static final String TEST_CONTENT_2 = "Hello again!";

	static TencentCosDocumentReader loader;

	static TencentCosDocumentReader batchLoader;

	static COSClient cosClient;

	DocumentParser parser = new TextDocumentParser();

	static {
		if (System.getenv("TENCENT_SECRET_ID") == null || System.getenv("TENCENT_SECRET_KEY") == null) {
			System.out.println(
					"TENCENT_SECRET_ID or TENCENT_SECRET_KEY environment variable is not set. Tests will be skipped.");
		}
	}

	@BeforeAll
	public static void beforeAll() {
		// Ensure environment variables are set, otherwise skip the test
		String secretId = System.getenv("TENCENT_SECRET_ID");
		String secretKey = System.getenv("TENCENT_SECRET_KEY");

		Assumptions.assumeTrue(secretId != null && !secretId.isEmpty(),
				"Skipping test because TENCENT_SECRET_ID is not set");
		Assumptions.assumeTrue(secretKey != null && !secretKey.isEmpty(),
				"Skipping test because TENCENT_SECRET_KEY is not set");

		TencentCredentials tencentCredentials = new TencentCredentials(secretId, secretKey, null);
		cosClient = new COSClient(tencentCredentials.toCredentialsProvider(),
				new ClientConfig(new Region("ap-shanghai")));
	}

	@Test
	void should_load_single_document() {
		// Ensure cosClient is initialized, otherwise skip the test
		Assumptions.assumeTrue(cosClient != null, "Skipping test because cosClient is not initialized");

		URL url = getClass().getClassLoader().getResource("test.txt");
		// given
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, new File(url.getFile())));

		// Get environment variables that have been validated in beforeAll
		String secretId = System.getenv("TENCENT_SECRET_ID");
		String secretKey = System.getenv("TENCENT_SECRET_KEY");

		// Skip test if environment variables are not set
		Assumptions.assumeTrue(secretId != null && !secretId.isEmpty() && secretKey != null && !secretKey.isEmpty(),
				"Skipping test because TENCENT_SECRET_ID or TENCENT_SECRET_KEY is not set");

		TencentCosResource tencentCosResource = TencentCosResource.builder()
			.secretId(secretId)
			.secretKey(secretKey)
			.region(new Region("ap-shanghai"))
			.bucket(TEST_BUCKET)
			.key(TEST_KEY)
			.build();
		// or
		TencentCosResource tencentCosResource2 = TencentCosResource.builder()
			.tencentCredentials(new TencentCredentials(secretId, secretKey, null))
			.region(new Region("ap-shanghai"))
			.bucket(TEST_BUCKET)
			.key(TEST_KEY)
			.build();

		TencentCosResource tencentCosResource3 = TencentCosResource.builder().cosClient(cosClient).build();

		loader = new TencentCosDocumentReader(tencentCosResource, parser);
		// when
		Document document = loader.get().get(0);

		// then
		assertThat(document.getText()).isEqualTo(TEST_CONTENT);
		assertThat(document.getMetadata()).hasSize(1);
		assertThat(document.getMetadata().get("source")).isEqualTo(String.format("cos://%s/%s", TEST_BUCKET, TEST_KEY));
	}

	@Test
	void should_load_multiple_documents() {
		// Ensure cosClient is initialized, otherwise skip the test
		Assumptions.assumeTrue(cosClient != null, "Skipping test because cosClient is not initialized");

		// given
		URL url = getClass().getClassLoader().getResource("test.txt");
		assert url != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, new File(url.getFile())));

		URL url2 = getClass().getClassLoader().getResource("test2.txt");
		assert url2 != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY_2, new File(url2.getFile())));

		// Get environment variables that have been validated in beforeAll
		String secretId = System.getenv("TENCENT_SECRET_ID");
		String secretKey = System.getenv("TENCENT_SECRET_KEY");

		// Skip test if environment variables are not set
		Assumptions.assumeTrue(secretId != null && !secretId.isEmpty() && secretKey != null && !secretKey.isEmpty(),
				"Skipping test because TENCENT_SECRET_ID or TENCENT_SECRET_KEY is not set");

		List<TencentCosResource> tencentCosResourceList = TencentCosResource.builder()
			.secretId(secretId)
			.secretKey(secretKey)
			.region(new Region("ap-shanghai"))
			.bucket(TEST_BUCKET)
			.buildBatch();

		batchLoader = new TencentCosDocumentReader(tencentCosResourceList, parser);

		// when
		List<Document> documents = batchLoader.get();

		// then
		assertThat(documents).hasSize(2);

		assertThat(documents.get(0).getText()).isEqualTo(TEST_CONTENT_2);
		assertThat(documents.get(0).getMetadata()).hasSize(1);
		assertThat(documents.get(0).getMetadata().get("source"))
			.isEqualTo(String.format("cos://%s/%s", TEST_BUCKET, TEST_KEY_2));

		assertThat(documents.get(1).getText()).isEqualTo(TEST_CONTENT);
		assertThat(documents.get(1).getMetadata()).hasSize(1);
		assertThat(documents.get(1).getMetadata().get("source"))
			.isEqualTo(String.format("cos://%s/%s", TEST_BUCKET, TEST_KEY));
	}

	@Test
	void should_load_multiple_documents_with_prefix() {
		// Ensure cosClient is initialized, otherwise skip the test
		Assumptions.assumeTrue(cosClient != null, "Skipping test because cosClient is not initialized");

		// given
		URL otherUrl = getClass().getClassLoader().getResource("other.txt");
		assert otherUrl != null;
		cosClient
			.putObject(new PutObjectRequest(TEST_BUCKET, "other_directory/file.txt", new File(otherUrl.getFile())));

		URL url = getClass().getClassLoader().getResource("test.txt");
		assert url != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, new File(url.getFile())));

		URL url2 = getClass().getClassLoader().getResource("test2.txt");
		assert url2 != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY_2, new File(url2.getFile())));

		// Get environment variables that have been validated in beforeAll
		String secretId = System.getenv("TENCENT_SECRET_ID");
		String secretKey = System.getenv("TENCENT_SECRET_KEY");

		// Skip test if environment variables are not set
		Assumptions.assumeTrue(secretId != null && !secretId.isEmpty() && secretKey != null && !secretKey.isEmpty(),
				"Skipping test because TENCENT_SECRET_ID or TENCENT_SECRET_KEY is not set");

		List<TencentCosResource> tencentCosResourceList = TencentCosResource.builder()
			.secretId(secretId)
			.secretKey(secretKey)
			.region(new Region("ap-shanghai"))
			.bucket(TEST_BUCKET)
			.prefix("test")
			.buildBatch();

		batchLoader = new TencentCosDocumentReader(tencentCosResourceList, parser);
		// when
		List<Document> documents = batchLoader.get();

		// then
		assertThat(documents).hasSize(2);
		assertThat(documents.get(0).getText()).isEqualTo(TEST_CONTENT_2);
		assertThat(documents.get(1).getText()).isEqualTo(TEST_CONTENT);
	}

}
