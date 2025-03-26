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

import { ChatOptions, ImageOptions } from './options';

export type ChatModelData = {
  name: string;
  model: string;
  modelType: ModelType;
  chatOptions: ChatOptions;
  imageOptions: ImageOptions;
};

/**
 * ModelRunActionParam
 */
export interface ModelRunActionParam {
  chatOptions?: null | ChatOptions;
  imageOptions?: null | ImageOptions;
  /**
   * user input
   */
  input?: string;
  /**
   * action key, bean name
   */
  key?: string;
  /**
   * system prompt
   */
  prompt?: string;
  /**
   * use stream response
   */
  stream?: boolean;
  [property: string]: any;
}

export type ChatModelRunResult = {
  input: ModelRunActionParam;
  result: ActionResult;
  telemetry: TelemetryResult;
  [property: string]: any;
};

/**
 * ActionResult
 */
export interface ActionResult {
  response?: string;
  /**
   * stream response
   */
  streamResponse?: Array<string>;
  [property: string]: any;
}

/**
* TelemetryResult
*/
export interface TelemetryResult {
  traceId?: string;
  [property: string]: any;
}


export enum ModelType {
  CHAT = 'CHAT',
  IMAGE = 'IMAGE',
}
