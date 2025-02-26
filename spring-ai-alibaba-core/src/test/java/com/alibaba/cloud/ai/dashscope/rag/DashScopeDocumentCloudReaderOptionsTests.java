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
package com.alibaba.cloud.ai.dashscope.rag;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeDocumentCloudReaderOptions.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentCloudReaderOptionsTests {

    @Test
    void testDefaultConstructor() {
        // 测试默认构造函数
        DashScopeDocumentCloudReaderOptions options = new DashScopeDocumentCloudReaderOptions();

        // 验证默认值是否为 "default"
        assertThat(options.getCategoryId()).isEqualTo("default");
    }

    @Test
    void testParameterizedConstructor() {
        // 测试带参数的构造函数
        String customCategoryId = "custom-category";
        DashScopeDocumentCloudReaderOptions options = new DashScopeDocumentCloudReaderOptions(customCategoryId);

        // 验证自定义值是否正确设置
        assertThat(options.getCategoryId()).isEqualTo(customCategoryId);
    }
}
