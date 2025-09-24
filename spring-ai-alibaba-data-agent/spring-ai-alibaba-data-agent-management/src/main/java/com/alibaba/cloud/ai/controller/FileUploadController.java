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
package com.alibaba.cloud.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.alibaba.cloud.ai.config.FileUploadProperties;
import com.alibaba.cloud.ai.controller.dto.UploadResponse;

/**
 * 文件上传控制器
 *
 * @author Makoto
 * @since 2025/9/19
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileUploadProperties fileUploadProperties;

    /**
     * 上传头像图片
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(UploadResponse.error("只支持图片文件"));
            }
            // 创建上传目录
            Path uploadDir = Paths.get(fileUploadProperties.getPath(), "avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 校验文件大小
            long maxImageSize = fileUploadProperties.getImageSize();
            if (file.getSize() > maxImageSize) {
                return ResponseEntity.badRequest().body(UploadResponse.error("图片大小超限，最大允许：" + maxImageSize + " 字节"));
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // 生成访问URL
            String fileUrl = "http://localhost:8065" + fileUploadProperties.getUrlPrefix() + "/avatars/" + filename;

            return ResponseEntity.ok(UploadResponse.ok("上传成功", fileUrl, filename));

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return ResponseEntity.internalServerError().body(UploadResponse.error("上传失败: " + e.getMessage()));
        }
    }

}
