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
import chatModelsService from '@/services/chat_models';
import { ChatModelData } from '@/types/chat_model';
import ChatModel from './ChatModel';

type Params = {
  model_name: string;
};

export default function Model() {
  // 路径参数
  const params = useParams<Params>();

  const [modelData, setModelData] = useState<ChatModelData>();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const modelData = await chatModelsService.getChatModelByName(
          params.model_name as string,
        );
        setModelData(modelData);
      } catch (error) {
        console.error('Failed to fetch chat models: ', error);
      }
    };
    fetchData();
  }, [params]);

  return modelData ? (
    <div style={{ padding: 20, height: '100%' }}>
      <ChatModel modelData={modelData} modeType={modelData.modelType} />
    </div>
  ) : (
    <p>加载中...</p>
  );
}
