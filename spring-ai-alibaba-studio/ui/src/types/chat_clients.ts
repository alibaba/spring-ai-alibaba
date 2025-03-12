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

import { ActionResult, ChatModelData, TelemetryResult } from './chat_model';
import { ChatOptions } from './options';

export type ChatClientData = {
  name: string;
  defaultSystemText: string;
  defaultSystemParams: any;
  chatModel: ChatModelData;
  isMemoryEnabled: boolean;
};

/**
 * ClientRunActionParam
 */
export interface ClientRunActionParam {
  /**
   * chat id use for chat mode, if not set, server will set a new
   */
  chatID?: null | string;
  chatOptions?: null | ChatOptions;
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

export interface ChatClientRunResult {
  chatID: string;
  input: ClientRunActionParam;
  result: ActionResult;
  telemetry: TelemetryResult;
  [property: string]: any;
}