/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.utils;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Reflectively copies bean properties from source to target, skipping any property whose
 * copy fails, instead of aborting the whole copy.
 *
 * <p>Used to clone a provider ChatOptions into a metadata-aware subclass (see the
 * {@code *ObservationMetadataChatOptions} classes in
 * {@code admin.service.client}). Unlike {@link org.springframework.beans.BeanUtils#copyProperties},
 * it tolerates properties inherited from a spring-ai interface that are not safely
 * invokable on the subclass - e.g. {@code outputSchema}, added to
 * {@code StructuredOutputChatOptions} in spring-ai 1.1.2, which previously made the bulk
 * copy throw {@code FatalBeanException("Could not copy property 'outputSchema' from source
 * to target")}.
 *
 * <p>Only the scalar model parameters plus the caller-supplied observation metadata need
 * to survive; vendor structured-output / tooling options are out of scope and silently
 * skipped.
 *
 * @since 1.0.0.3
 */
public final class ObservationOptionsCopyUtils {

    private ObservationOptionsCopyUtils() {
    }

    /**
     * Copy readable, non-null properties from source to target, skipping any property
     * that cannot be reflectively copied.
     * @param source the bean to copy from
     * @param target the bean to copy into
     */
    public static void copySafely(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        BeanWrapper src = new BeanWrapperImpl(source);
        BeanWrapper dst = new BeanWrapperImpl(target);
        for (PropertyDescriptor pd : dst.getPropertyDescriptors()) {
            String name = pd.getName();
            if (pd.getWriteMethod() == null || "class".equals(name)) {
                continue;
            }
            try {
                Object value = src.getPropertyValue(name);
                if (value == null) {
                    continue; // unset inherited properties (e.g. outputSchema) are irrelevant
                }
                dst.setPropertyValue(name, value);
            }
            catch (Exception ignored) {
                // skip a property that cannot be reflectively copied
            }
        }
    }

}
