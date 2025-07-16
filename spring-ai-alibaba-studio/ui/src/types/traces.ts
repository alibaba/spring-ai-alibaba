///
/// Copyright 2024-2025 the original author or authors.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///      https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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

export interface TraceInfo {
  id: string;
  latencyMilliseconds: string;
  model: string;
  promptTokens: number;
  completionTokens: string;
  totalTokens: string;
  input: string;
  output: string;
  tags: string[];
  calculatedTotalCost: string;
  calculatedInputCost: string;
  calculatedOutputCost: string;
  usageDetails: {
      input: string;
      output: string;
      total: string;
  };
  timestamp: string;
  costDetails: {
      input: string;
      output: string;
      total: string;
  };
  traceDetail: TraceDetail;
}

export interface TraceDetail {
  title: string;
  input: string;
  output: string;
  key: string;
  detail: object;
  costTime: number;
  attributes: object;
  children: TraceDetail[];
}
