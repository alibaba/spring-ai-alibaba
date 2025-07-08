/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.node.annotation;

import com.alibaba.cloud.ai.example.deepresearch.model.NodeDefinition;
import com.alibaba.cloud.ai.example.deepresearch.node.AbstractNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * Injects node definition information into the node. This is used to automatically
 * populate the node's definition based on the @Node annotation present on the node class.
 *
 * @author ViliamSun
 * @since 1.0.0
 */

@Component
public class NodeDefinitionInjector implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) {
		if (bean instanceof AbstractNode node) {
			Node nodeInfo = AnnotationUtils.findAnnotation(bean.getClass(), Node.class);
			if (nodeInfo != null) {
				NodeDefinition def = new NodeDefinition();
				def.setName(nodeInfo.name());
				def.setDescription(nodeInfo.description());
				node.setNodeDefinition(def);
			}
		}
		return bean;
	}

}
