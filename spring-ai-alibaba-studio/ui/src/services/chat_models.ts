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

import {
  ChatModelData,
  ChatModelRunResult,
  ModelType,
  ModelRunActionParam,
} from '@/types/chat_model';
import { request } from 'ice';

export default {
  // 获取 ChatModels 列表
  async getChatModels(): Promise<ChatModelData[]> {
    return await request({
      url: 'studio/api/chat-models',
      method: 'get',
    });
  },

  // 根据 Model name 获取 ChatModel
  async getChatModelByName(name: string): Promise<ChatModelData> {
    return await request({
      url: `studio/api/chat-models/${name}`,
      method: 'get',
    });
  },

  // 根据 Model name 获取 ChatModel
  async postChatModel(data: ModelRunActionParam): Promise<ChatModelRunResult> {
    return await request({
      url: '/studio/api/chat-models',
      method: 'post',
      data,
    });
  },

  // 根据 Model name 获取 ChatModel
  async postImageModel(data: ModelRunActionParam): Promise<ChatModelRunResult> {
    return await request({
      url: '/studio/api/chat-models/run/image-gen/url',
      method: 'post',
      data,
    });
  },

  async getModelNames(modelType: ModelType): Promise<string[]> {
    return await request({
      url: `/studio/api/chat-models/model-names?modelType=${modelType}`,
      method: 'get',
    });
  },
};
