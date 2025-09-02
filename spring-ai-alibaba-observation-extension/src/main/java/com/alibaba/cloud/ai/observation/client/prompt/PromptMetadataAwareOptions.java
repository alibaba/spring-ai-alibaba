package com.alibaba.cloud.ai.observation.client.prompt;

import org.springframework.lang.Nullable;

public interface PromptMetadataAwareOptions {

  @Nullable
  String getPromptVersion();

  @Nullable
  String getPromptKey();

  @Nullable
  default String getPromptTemplate() {
    return null;
  }
}
