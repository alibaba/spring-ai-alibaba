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
 * @since 2024-11-27 21:41
 */
@EnabledIfEnvironmentVariable(named = "TENCENT_SECRET_KEY", matches = ".+")
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

	@BeforeAll
	public static void beforeAll() {
		TencentCredentials tencentCredentials = new TencentCredentials(System.getenv("TENCENT_SECRET_ID"),
				System.getenv("TENCENT_SECRET_KEY"), null);
		cosClient = new COSClient(tencentCredentials.toCredentialsProvider(),
				new ClientConfig(new Region("ap-shanghai")));
	}

	@Test
	void should_load_single_document() {

		URL url = getClass().getClassLoader().getResource("test.txt");
		// given
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, new File(url.getFile())));

		TencentCosResource tencentCosResource = TencentCosResource.builder()
			.secretId(System.getenv("TENCENT_SECRET_ID"))
			.secretKey(System.getenv("TENCENT_SECRET_KEY"))
			.region(new Region("ap-shanghai"))
			.bucket(TEST_BUCKET)
			.key(TEST_KEY)
			.build();
		// or
		TencentCosResource tencentCosResource2 = TencentCosResource.builder()
			.tencentCredentials(new TencentCredentials(System.getenv("TENCENT_SECRET_ID"),
					System.getenv("TENCENT_SECRET_KEY"), null))
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

		// given
		URL url = getClass().getClassLoader().getResource("test.txt");
		assert url != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY, new File(url.getFile())));

		URL url2 = getClass().getClassLoader().getResource("test2.txt");
		assert url2 != null;
		cosClient.putObject(new PutObjectRequest(TEST_BUCKET, TEST_KEY_2, new File(url2.getFile())));

		List<TencentCosResource> tencentCosResourceList = TencentCosResource.builder()
			.secretId(System.getenv("TENCENT_SECRET_ID"))
			.secretKey(System.getenv("TENCENT_SECRET_KEY"))
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

		List<TencentCosResource> tencentCosResourceList = TencentCosResource.builder()
			.secretId(System.getenv("TENCENT_SECRET_ID"))
			.secretKey(System.getenv("TENCENT_SECRET_KEY"))
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
