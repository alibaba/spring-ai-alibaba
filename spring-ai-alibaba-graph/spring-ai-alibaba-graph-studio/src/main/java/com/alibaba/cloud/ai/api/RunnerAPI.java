package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.AppRunEvent;
import com.alibaba.cloud.ai.service.runner.AppRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.Map;

@Tag(name = "Runner", description = "the app running api")
public interface RunnerAPI {

	AppRunner getRunner();

	@Operation(summary = "run app in stream mode", tags = { "app-run" })
	@PostMapping(value = "/{id}/stream")
	default Flux<AppRunEvent> stream(@PathVariable String id, @RequestBody Map<String, Object> inputs) {
		return getRunner().stream(id, inputs);
	}

	@Operation(summary = "run app in sync mode", tags = { "app-run" })
	@PostMapping(value = "/{id}/sync")
	default R<AppRunEvent> run(@PathVariable String id, @RequestBody Map<String, Object> inputs) {
		return R.success(getRunner().run(id, inputs));
	}

}
