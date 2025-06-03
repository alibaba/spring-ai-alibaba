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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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


    private OkHttpClient mockOkHttpClient;

    private Call mockCall;

    private Response mockResponse;


    private static final String TEST_FILE_NAME = "test.xlsx";
    private static final String TEST_URL = "https://test-url.com";


    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // 创建一个临时Excel文件用于测试
        File excelFile = tempDir.resolve(TEST_FILE_NAME).toFile();
        try (FileOutputStream fos = new FileOutputStream(excelFile)) {
            // 写入一些假数据，模拟Excel文件
            fos.write(new byte[]{0x50, 0x4B, 0x03, 0x04}); // Excel文件的魔数
        }
        mockFile = excelFile;

        // 初始化UploadLeaseResponse相关对象
        Map<String, String> headers = new HashMap<>();
        headers.put("X-bailian-extra", "MTAwNTQyNjQ5NTE2OTE3OA==");

        mockParamData = new DashScopeApi.UploadLeaseResponse.UploadLeaseParamData(
                TEST_URL, "PUT", headers);

        DashScopeApi.UploadLeaseResponse.UploadLeaseResponseData responseData =
                new DashScopeApi.UploadLeaseResponse.UploadLeaseResponseData(
                        "test-lease-id", "test-type", mockParamData);

        mockUploadLeaseResponse = new DashScopeApi.UploadLeaseResponse(
                "SUCCESS", "success", responseData);

        dashScopeApi = DashScopeApi.builder().apiKey("test-api-key").build();

        // 初始化Mock对象
        mockOkHttpClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        mockResponse = mock(Response.class);
    }

    /**
     * 测试uploadFile方法的正常执行流程 验证方法能够正确处理带有Content-Type的情况
     */
    @Test
    void testUploadFileWithContentType() throws Exception {
        try (MockedConstruction<OkHttpClient> mockedConstruction =
                     mockConstruction(OkHttpClient.class, (mockOkHttpClient, context) -> {
                         when(mockOkHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
                     })) {
            // 设置OkHttpClient的行为
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);

            // 创建一个ArgumentCaptor来捕获Request对象
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            // 通过反射访问私有方法
            Method uploadFileMethod = DashScopeApi.class.getDeclaredMethod("uploadFile", File.class, DashScopeApi.UploadLeaseResponse.class);
            uploadFileMethod.setAccessible(true);

            mockParamData.header().put("Content-Type", "application/pdf");
            // 执行方法
            uploadFileMethod.invoke(dashScopeApi, mockFile, mockUploadLeaseResponse);

            // 获取构造的 OkHttpClient 实例
            OkHttpClient constructedClient = mockedConstruction.constructed().get(0);

            verify(constructedClient).newCall(requestCaptor.capture());
            Request capturedRequest = requestCaptor.getValue();

            assertEquals("PUT", capturedRequest.method());
            assertNotNull(Objects.requireNonNull(capturedRequest.body()).contentType());
            assertEquals("MTAwNTQyNjQ5NTE2OTE3OA==", capturedRequest.header("X-bailian-extra"));

            verify(mockCall).execute();
        }
    }

    /**
     * 测试uploadFile方法的正常执行流程 验证方法能够正确处理没有Content-Type的情况
     */
    @Test
    void testUploadFileWithoutContentType() throws Exception {
        // 假设 mockParamData 是某个配置对象
        mockParamData.header().put("Content-Type", "");

        try (MockedConstruction<OkHttpClient> mockedConstruction =
                     mockConstruction(OkHttpClient.class, (mockOkHttpClient, context) -> {
                         when(mockOkHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
                     })) {
            // 设置OkHttpClient的行为
            when(mockCall.execute()).thenReturn(mockResponse);
            when(mockResponse.isSuccessful()).thenReturn(true);

            // 创建一个ArgumentCaptor来捕获Request对象
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            // 通过反射访问私有方法
            Method uploadFileMethod = DashScopeApi.class.getDeclaredMethod("uploadFile", File.class, DashScopeApi.UploadLeaseResponse.class);
            uploadFileMethod.setAccessible(true);

            // 执行方法
            uploadFileMethod.invoke(dashScopeApi, mockFile, mockUploadLeaseResponse);

            // 获取构造的 OkHttpClient 实例
            OkHttpClient constructedClient = mockedConstruction.constructed().get(0);

            verify(constructedClient).newCall(requestCaptor.capture());
            Request capturedRequest = requestCaptor.getValue();

            assertEquals("PUT", capturedRequest.method());
            assertEquals("", capturedRequest.header("Content-Type"));
            assertEquals("MTAwNTQyNjQ5NTE2OTE3OA==", capturedRequest.header("X-bailian-extra"));

            verify(mockCall).execute();
        }
    }

}