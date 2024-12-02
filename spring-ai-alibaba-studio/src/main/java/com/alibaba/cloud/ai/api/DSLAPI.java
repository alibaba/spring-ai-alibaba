package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.app.App;
import com.alibaba.cloud.ai.param.DSLParam;
import com.alibaba.cloud.ai.service.DSLAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "dsl", description = "the DSL API")
public interface DSLAPI {

	DSLAdapter getAdapter(String dialect);

	@Operation(summary = "export app to dsl", tags = { "dsl" })
	@GetMapping(value = "/export/{id}", produces = "application/json")
	default R<String> exportDSL(@PathVariable("id") String id, @RequestParam("dialect") String dialect) {
		return R.success(getAdapter(dialect).exportDSL(id));
	}

	@Operation(summary = "import app from dsl", tags = { "dsl" })
	@PostMapping(value = "/import", produces = "application/json")
	default R<App> importDSL(@RequestBody DSLParam param) {
		return R.success(getAdapter(param.getDialect()).importDSL(param.getContent()));
	}

}
