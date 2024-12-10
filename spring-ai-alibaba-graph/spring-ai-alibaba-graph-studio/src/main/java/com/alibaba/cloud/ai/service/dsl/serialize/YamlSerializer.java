package com.alibaba.cloud.ai.service.dsl.serialize;

import com.alibaba.cloud.ai.service.dsl.Serializer;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Component("yaml")
public class YamlSerializer implements Serializer {

	private final Yaml yaml;

	public YamlSerializer() {
		// configure DumperOptions to force block style
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		this.yaml = new Yaml(options);
	}

	@Override
	public Map<String, Object> load(String s) {
		return yaml.load(s);
	}

	@Override
	public String dump(Map<String, Object> data) {
		return yaml.dump(data);
	}

}
