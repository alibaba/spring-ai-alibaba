package com.alibaba.cloud.ai.example.manus.config;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping
    public ResponseEntity<List<ConfigEntity>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    @GetMapping("/{configPath}")
    public ResponseEntity<ConfigEntity> getConfig(@PathVariable String configPath) {
        return configService.getConfig(configPath)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{configPath}")
    public ResponseEntity<Void> updateConfig(
            @PathVariable String configPath,
            @RequestBody Map<String, String> payload) {
        configService.updateConfig(configPath, payload.get("value"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{configPath}/reset")
    public ResponseEntity<Void> resetConfig(@PathVariable String configPath) {
        configService.resetConfig(configPath);
        return ResponseEntity.ok().build();
    }
}
