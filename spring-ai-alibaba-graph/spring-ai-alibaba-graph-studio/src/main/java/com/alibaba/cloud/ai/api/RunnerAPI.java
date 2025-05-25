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
