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

import { useEffect, useState } from 'react';
import { useParams } from 'ice';
import chatClientsService from '@/services/chat_clients';
import styles from './index.module.css';
import { Spin } from 'antd';
import ChatClient from '.';
import { ChatClientData } from '@/types/chat_clients';

type Params = {
  client_name: string;
};

export default function Client() {
  // 路径参数
  const params = useParams<Params>();

  const [clientData, setChatClientData] = useState<ChatClientData>();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const chatClientData = await chatClientsService.getChatClientByName(
          params.client_name as string,
        );
        setChatClientData(chatClientData);
      } catch (error) {
        console.error('Failed to fetch chat models: ', error);
      }
    };
    fetchData();
  }, [params]);

  return clientData ? (
    <div style={{ padding: 20, height: '100%' }}>
      <ChatClient
        chatClientData={clientData}
      />
    </div>
  ) : (
    <div className={styles['container']}>
      <Spin tip="Loading">
        <div className={styles['message-loading']} />
      </Spin>
    </div>
  );
}
