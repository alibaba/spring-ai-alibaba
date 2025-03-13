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
import com.alibaba.cloud.ai.exception.NotFoundException;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.RunEvent;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.runner.Runner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

@Tag(name = "Runner", description = "the running api")
public interface RunnerAPI {

	Runner getRunner(String type);

	AppSaver getAppSaver();

	@Operation(summary = "run app in stream mode", tags = { "Runner" })
	@PostMapping(value = "/app/{id}/stream")
	default Flux<RunEvent> stream(@PathVariable String id, @RequestBody Map<String, Object> inputs) {
		App app = Optional.ofNullable(getAppSaver().get(id)).orElseThrow(() -> new NotFoundException("app not found"));
		Runner runner = Optional.ofNullable(getRunner(app.getMetadata().getMode()))
			.orElseThrow(() -> new NotImplementedException("not supported yet "));
		return runner.stream(app, inputs);
	}

	@Operation(summary = "run app in sync mode", tags = { "Runner" })
	@PostMapping(value = "/app/{id}/sync")
	default R<RunEvent> run(@PathVariable String id, @RequestBody Map<String, Object> inputs) {
		App app = Optional.ofNullable(getAppSaver().get(id)).orElseThrow(() -> new NotFoundException("app not found"));
		Runner runner = Optional.ofNullable(getRunner(app.getMetadata().getMode()))
			.orElseThrow(() -> new NotImplementedException("not supported yet "));
		return R.success(runner.run(app, inputs));
	}

}
