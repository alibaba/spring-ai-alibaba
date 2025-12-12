/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.util.Assert;

import java.beans.ConstructorProperties;
import java.util.*;

/**
 * auth: dahua
 */
@JsonIgnoreProperties(ignoreUnknown = true, value = {"@class"})
public class CustomChatGenerationMetadata implements ChatGenerationMetadata {

    private final Map<String, Object> metadata;
    private final String finishReason;
    private final Set<String> contentFilters;

    @ConstructorProperties({"metadata", "finishReason", "contentFilters"})
    public CustomChatGenerationMetadata(Map<String, Object> metadata, String finishReason, Set<String> contentFilters) {
        Assert.notNull(metadata, "Metadata must not be null");
        Assert.notNull(contentFilters, "Content filters must not be null");
        this.metadata = metadata;
        this.finishReason = finishReason;
        this.contentFilters = new HashSet(contentFilters);
    }

    public <T> T get(String key) {
        return (T) this.metadata.get(key);
    }

    public boolean containsKey(String key) {
        return this.metadata.containsKey(key);
    }

    public <T> T getOrDefault(String key, T defaultObject) {
        return (T) (this.containsKey(key) ? this.get(key) : defaultObject);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(this.metadata.entrySet());
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.metadata.keySet());
    }

    public boolean isEmpty() {
        return this.metadata.isEmpty();
    }

    public String getFinishReason() {
        return this.finishReason;
    }

    public Set<String> getContentFilters() {
        return Collections.unmodifiableSet(this.contentFilters);
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.metadata, this.finishReason, this.contentFilters});
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            CustomChatGenerationMetadata other = (CustomChatGenerationMetadata) obj;
            return Objects.equals(this.metadata, other.metadata) && Objects.equals(this.finishReason, other.finishReason) && Objects.equals(this.contentFilters, other.contentFilters);
        } else {
            return false;
        }
    }

    public String toString() {
        return String.format("CustomChatGenerationMetadata[finishReason='%s', filters=%d, metadata=%d]", this.finishReason, this.contentFilters.size(), this.metadata.size());
    }
}
