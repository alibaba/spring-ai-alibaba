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
