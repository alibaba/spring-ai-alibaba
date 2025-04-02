package com.alibaba.cloud.ai.example.manus.config;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

	@Autowired
	private ConfigService configService;

	@GetMapping("/group/{groupName}")
	public ResponseEntity<List<ConfigEntity>> getConfigsByGroup(@PathVariable String groupName) {
		return ResponseEntity.ok(configService.getConfigsByGroup(groupName));
	}

	@PostMapping("/batch-update")
	public ResponseEntity<Void> batchUpdateConfigs(@RequestBody List<ConfigEntity> configs) {
		configService.batchUpdateConfigs(configs);
		return ResponseEntity.ok().build();
	}

}
