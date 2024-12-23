/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dotprompt;

import com.github.jknack.handlebars.Parser;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.TemplateSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class LRUTemplateCache implements TemplateCache {

	private final Map<String, Template> cache;

	public LRUTemplateCache(int maxSize) {
		this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Template> eldest) {
				return size() > maxSize;
			}
		};
	}

	@Override
	public void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}

	@Override
	public void evict(TemplateSource templateSource) {
		synchronized (cache) {
			try {
				cache.remove(templateSource.content(StandardCharsets.UTF_8));
			}
			catch (IOException ignored) {
			}
		}
	}

	@Override
	public Template get(TemplateSource templateSource, Parser parser) throws IOException {
		synchronized (cache) {
			String content = templateSource.content(StandardCharsets.UTF_8);
			Template template = cache.get(content);
			if (template == null) {
				template = parser.parse(templateSource);
				cache.put(content, template);
			}
			return template;
		}
	}

	@Override
	public TemplateCache setReload(boolean b) {
		return null;
	}

}
