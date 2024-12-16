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

import { ChatClientData } from '@/types/chat_clients';
import { request } from 'ice';

export default {
  // 获取ChatClients列表
  async getChatClients(): Promise<ChatClientData[]> {
    return await request({
      url: '/studio/api/chat-clients',
      method: 'get',
    });
  },

  // 根据chat client name获取ChatClient
  async getChatClientByName(name: string): Promise<ChatClientData> {
    return await request({
      url: `/studio/api/chat-clients/${name}`,
      method: 'get',
    });
  },
};
