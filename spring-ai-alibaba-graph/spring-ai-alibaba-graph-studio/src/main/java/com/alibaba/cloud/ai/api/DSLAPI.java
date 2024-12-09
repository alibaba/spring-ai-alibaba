package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.DSLParam;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "DSL", description = "the DSL API")
public interface DSLAPI {

	DSLAdapter getAdapter(String dialect);

	AppSaver getAppSaver();

	@Operation(summary = "export app to dsl", tags = { "DSL" })
	@GetMapping(value = "/export/{id}", produces = "application/json")
	default R<String> exportDSL(@PathVariable("id") String id, @RequestParam("dialect") String dialect) {
		App app = Optional.ofNullable(getAppSaver().get(id)).orElseThrow(()-> new IllegalArgumentException("App not found: " + id));
		return R.success(getAdapter(dialect).exportDSL(app));
	}

	@Operation(summary = "import app from dsl", tags = { "DSL" })
	@PostMapping(value = "/import", produces = "application/json")
	default R<App> importDSL(@RequestBody DSLParam param) {
		App app = getAdapter(param.getDialect()).importDSL(param.getContent());
		app = getAppSaver().save(app);
		return R.success(app);
	}

}
