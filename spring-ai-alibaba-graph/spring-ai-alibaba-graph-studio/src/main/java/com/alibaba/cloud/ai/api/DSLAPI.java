/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.exception.SerializationException;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.DSLParam;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Tag(name = "DSL", description = "the DSL API")
public interface DSLAPI {

	DSLAdapter getAdapter(DSLDialectType dialect);

	AppSaver getAppSaver();

	@Operation(summary = "export app to dsl", tags = { "DSL" })
	@GetMapping(value = "/export/{id}", produces = "application/json")
	default R<String> exportDSL(@PathVariable("id") String id, @RequestParam("dialect") String dialect) {
		App app = Optional.ofNullable(getAppSaver().get(id))
			.orElseThrow(() -> new IllegalArgumentException("App not found: " + id));
		DSLDialectType dialectType = DSLDialectType.fromValue(dialect)
			.orElseThrow(() -> new NotImplementedException("Unsupported dsl dialect: " + dialect));
		return R.success(getAdapter(dialectType).exportDSL(app));
	}

	@Operation(summary = "export app to dsl file", tags = { "DSL" })
	@GetMapping(value = "/export-file/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	default ResponseEntity<Resource> exportDSLFile(@PathVariable("id") String id,
			@RequestParam("dialect") String dialect) {
		App app = Optional.ofNullable(getAppSaver().get(id))
			.orElseThrow(() -> new IllegalArgumentException("App not found: " + id));
		DSLDialectType dialectType = DSLDialectType.fromValue(dialect)
			.orElseThrow(() -> new NotImplementedException("Unsupported dsl dialect: " + dialect));
		String dslContent = getAdapter(dialectType).exportDSL(app);
		ByteArrayResource resource = new ByteArrayResource(dslContent.getBytes(StandardCharsets.UTF_8));
		String fileName = app.getMetadata().getName() + dialectType.fileExtension();
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.contentLength(resource.contentLength())
			.body(resource);
	}

	@Operation(summary = "import app from dsl", tags = { "DSL" })
	@PostMapping(value = "/import", produces = "application/json")
	default R<App> importDSL(@RequestBody DSLParam param) {
		DSLDialectType dialectType = DSLDialectType.fromValue(param.getDialect())
			.orElseThrow(() -> new NotImplementedException("Unsupported dsl dialect: " + param.getDialect()));
		App app = getAdapter(dialectType).importDSL(param.getContent());
		app = getAppSaver().save(app);
		return R.success(app);
	}

	@Operation(summary = "import app from dsl file ", tags = { "DSL" })
	@PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
	default R<App> importDSLFile(@RequestPart("file") MultipartFile file, @RequestParam("dialect") String dialect) {
		String content;
		try {
			content = new String(file.getBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new SerializationException("Read dsl file failed, please check if the encoding of file is UTF_8 ");
		}
		DSLDialectType dialectType = DSLDialectType.fromValue(dialect)
			.orElseThrow(() -> new NotImplementedException("Unsupported dsl dialect: " + dialect));
		App app = getAdapter(dialectType).importDSL(content);
		app = getAppSaver().save(app);
		return R.success(app);
	}

}
