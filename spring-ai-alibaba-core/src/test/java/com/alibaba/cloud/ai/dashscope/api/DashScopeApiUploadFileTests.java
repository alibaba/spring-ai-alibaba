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
package com.alibaba.cloud.ai.dashscope.api;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试DashScopeApi类中的uploadFile方法
 *
 * @author joe
 */
class DashScopeApiUploadFileTests {

	private DashScopeApi dashScopeApi;

	private File mockFile;

	private DashScopeApi.UploadLeaseResponse mockUploadLeaseResponse;

	private DashScopeApi.UploadLeaseResponse.UploadLeaseParamData mockParamData;

	private Call mockCall;

	private Response mockResponse;

	private static final String TEST_FILE_NAME = "test.xlsx";

	private static final String TEST_URL = "https://test-url.com";

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		File excelFile = tempDir.resolve(TEST_FILE_NAME).toFile();
		try (FileOutputStream fos = new FileOutputStream(excelFile)) {
			fos.write("你好".getBytes(StandardCharsets.UTF_8));
		}
		mockFile = excelFile;

		Map<String, String> headers = new HashMap<>();
		headers.put("X-bailian-extra", "test-extra-key");

		mockParamData = new DashScopeApi.UploadLeaseResponse.UploadLeaseParamData(TEST_URL, "PUT", headers);

		DashScopeApi.UploadLeaseResponse.UploadLeaseResponseData responseData = new DashScopeApi.UploadLeaseResponse.UploadLeaseResponseData(
				"test-lease-id", "test-type", mockParamData);

		mockUploadLeaseResponse = new DashScopeApi.UploadLeaseResponse("SUCCESS", "success", responseData);

		dashScopeApi = DashScopeApi.builder().apiKey("test-api-key").build();

		mockCall = Mockito.mock(Call.class);
		mockResponse = Mockito.mock(Response.class);
	}

	/**
	 * Test the normal execution flow of the uploadFile method Verify that the method
	 * handles the case with Content-Type correctly
	 */
	@Test
	void testUploadFileWithContentType() throws Exception {
		try (MockedConstruction<OkHttpClient> mockedConstruction = mockConstruction(OkHttpClient.class,
				(mockOkHttpClient, context) -> {
					when(mockOkHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
				})) {
			when(mockCall.execute()).thenReturn(mockResponse);
			when(mockResponse.isSuccessful()).thenReturn(true);

			ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

			Method uploadFileMethod = DashScopeApi.class.getDeclaredMethod("uploadFile", File.class,
					DashScopeApi.UploadLeaseResponse.class);
			uploadFileMethod.setAccessible(true);

			mockParamData.header().put("Content-Type", "application/pdf");
			uploadFileMethod.invoke(dashScopeApi, mockFile, mockUploadLeaseResponse);

			OkHttpClient constructedClient = mockedConstruction.constructed().get(0);

			verify(constructedClient).newCall(requestCaptor.capture());
			Request capturedRequest = requestCaptor.getValue();

			assertEquals("PUT", capturedRequest.method());
			assertNotNull(Objects.requireNonNull(capturedRequest.body()).contentType());
			assertEquals("test-extra-key", capturedRequest.header("X-bailian-extra"));

			verify(mockCall).execute();
		}
	}

	/**
	 * Test the normal execution flow of the uploadFile method Verify that the method
	 * handles the absence of a Content-Type correctly
	 */
	@Test
	void testUploadFileWithoutContentType() throws Exception {
		mockParamData.header().put("Content-Type", "");

		try (MockedConstruction<OkHttpClient> mockedConstruction = mockConstruction(OkHttpClient.class,
				(mockOkHttpClient, context) -> {
					when(mockOkHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
				})) {
			when(mockCall.execute()).thenReturn(mockResponse);
			when(mockResponse.isSuccessful()).thenReturn(true);

			ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

			Method uploadFileMethod = DashScopeApi.class.getDeclaredMethod("uploadFile", File.class,
					DashScopeApi.UploadLeaseResponse.class);
			uploadFileMethod.setAccessible(true);

			uploadFileMethod.invoke(dashScopeApi, mockFile, mockUploadLeaseResponse);

			OkHttpClient constructedClient = mockedConstruction.constructed().get(0);

			verify(constructedClient).newCall(requestCaptor.capture());
			Request capturedRequest = requestCaptor.getValue();

			assertEquals("PUT", capturedRequest.method());
			assertEquals("", capturedRequest.header("Content-Type"));
			assertEquals("test-extra-key", capturedRequest.header("X-bailian-extra"));

			verify(mockCall).execute();
		}
	}

}
