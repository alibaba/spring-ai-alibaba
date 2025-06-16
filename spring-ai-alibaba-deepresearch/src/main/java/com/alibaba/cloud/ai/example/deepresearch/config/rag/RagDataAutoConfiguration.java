/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.config.rag;

import com.alibaba.cloud.ai.example.deepresearch.service.VectorStoreDataIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles automated data ingestion for RAG. - Ingests initial data on application
 * startup. - Schedules periodic scanning of a directory for new documents.
 *
 * @author hupei
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "enabled", havingValue = "true")
public class RagDataAutoConfiguration implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(RagDataAutoConfiguration.class);

	private final VectorStoreDataIngestionService ingestionService;

	private final RagProperties ragProperties;

	private final ResourcePatternResolver resourcePatternResolver;

	public RagDataAutoConfiguration(VectorStoreDataIngestionService ingestionService, RagProperties ragProperties,
			ResourcePatternResolver resourcePatternResolver) {
		this.ingestionService = ingestionService;
		this.ragProperties = ragProperties;
		this.resourcePatternResolver = resourcePatternResolver;
	}

	/**
	 * 1. 应用启动时执行，加载初始化数据
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<String> locations = ragProperties.getData().getLocations();
		if (locations == null || locations.isEmpty()) {
			logger.info("No initial data locations configured. Skipping startup ingestion.");
			return;
		}

		logger.info("Starting initial data ingestion from locations: {}", locations);
		List<Resource> allResources = new ArrayList<>();
		for (String location : locations) {
			allResources.addAll(Arrays.asList(resourcePatternResolver.getResources(location)));
		}
		ingestionService.ingest(allResources);
		logger.info("Initial data ingestion complete.");
	}

	/**
	 * 2. 定时任务，扫描指定目录
	 */
	@Scheduled(cron = "${spring.ai.alibaba.deepresearch.rag.data.scan.cron:0 0 * * * *}")
	@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX + ".data.scan", name = "enabled", havingValue = "true")
	public void scheduledIngest() {
		RagProperties.Data.Scan scanConfig = ragProperties.getData().getScan();
		String scanDir = scanConfig.getDirectory();
		String archiveDir = scanConfig.getArchiveDirectory();

		if (!StringUtils.hasText(scanDir) || !StringUtils.hasText(archiveDir)) {
			logger.warn("Scan directory or archive directory is not configured. Skipping scheduled ingestion.");
			return;
		}

		Path scanPath = Paths.get(scanDir);
		Path archivePath = Paths.get(archiveDir);

		if (!Files.isDirectory(scanPath)) {
			logger.error("Scan path is not a directory: {}. Please create it.", scanDir);
			return;
		}

		try {
			Files.createDirectories(archivePath);
			logger.debug("Scanning directory for new documents: {}", scanDir);
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(scanPath)) {
				for (Path filePath : stream) {
					if (Files.isRegularFile(filePath)) {
						ingestionService.ingest(new FileSystemResource(filePath.toFile()));
						Files.move(filePath, archivePath.resolve(filePath.getFileName()),
								StandardCopyOption.REPLACE_EXISTING);
						logger.info("Moved processed file {} to archive.", filePath.getFileName());
					}
				}
			}
		}
		catch (IOException e) {
			logger.error("Error during scheduled directory scan.", e);
		}
	}

}
