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
package com.alibaba.cloud.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传相关配置属性。
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.nl2sql.file.upload")
public class FileUploadProperties {

    /**
     * 本地上传目录路径。
     */
    private String path = "./uploads";

    /**
     * 对外暴露的访问前缀。
     */
    private String urlPrefix = "/uploads";

    /**
     * 头像图片大小上限（字节）。默认 2MB。
     */
    private long imageSize = 2L * 1024 * 1024;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public long getImageSize() {
        return imageSize;
    }

    public void setImageSize(long imageSize) {
        this.imageSize = imageSize;
    }
}


