package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.app.App;
import com.alibaba.cloud.ai.param.CreateAppParam;
import com.alibaba.cloud.ai.service.AppDelegate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "app", description = "the app API")
public interface AppAPI {

	AppDelegate getDelegate();

	@Operation(summary = "create app", tags = { "app" })
	@PostMapping(value = "", produces = "application/json")
	default R<App> create(@RequestBody CreateAppParam param) {
		return R.success(getDelegate().create(param));
	}

	@Operation(summary = "list apps", tags = { "app" })
	@GetMapping(value = "", produces = "application/json")
	default R<List<App>> list() {
		return R.success(getDelegate().list());
	}

	@Operation(summary = "get app by id", tags = { "app" })
	@GetMapping(value = "/{id}", produces = "application/json")
	default R<App> get(@PathVariable String id) {
		return R.success(getDelegate().get(id));
	}

	@Operation(summary = "sync app", tags = { "app" })
	@PutMapping(value = "", produces = "application/json")
	default R<App> sync(@RequestBody App app) {
		return R.success(getDelegate().sync(app));
	}

	@Operation(summary = "delete app", tags = { "app" })
	@DeleteMapping(value = "/{id}", produces = "application/json")
	default R<Boolean> delete(@PathVariable String id) {
		return R.success(true);
	}


}
