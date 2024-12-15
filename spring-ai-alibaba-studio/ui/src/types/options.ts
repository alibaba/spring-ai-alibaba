/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export type ImageOptions = {
  responseFormat: string;
  model: string;
  n: number;
  size: string;
  style: string;
  seed: number;
  ref_img?: string;
  ref_strength?: number;
  ref_mode?: string;
  negative_prompt?: string;
};

export type ChatOptions = {
  model: string;
  temperature: number;
  seed: number;
  top_p: number;
  top_k: number;
  stop: Record<string, unknown>[];
  enable_search: boolean;
  incremental_output: boolean;
  repetition_penalty: number;
  prompt?: string;
  tools: {
    type: 'function';
    function: {
      description: string;
      name: string;
      parameters: {
        property1: Record<string, unknown>;
        property2: Record<string, unknown>;
      };
    };
  }[];
  maxTokens?: number;
  presencePenalty?: number;
  frequencyPenalty?: number;
  stopSequences?: string[];
  proxyToolCalls?: boolean;
  tool_choice?: Record<string, unknown>;
  vl_high_resolution_images?: boolean;
  multi_model?: boolean;
};