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

import { useEffect, useState, memo } from 'react';
import { Empty, Flex } from 'antd';
import { useParams } from 'ice';
import { ChatModelData, ChatModelRunResult, ModelType } from '@/types/chat_model';
import { RightPanelValues } from '@/components/right_panel/types';
import Chat from '@/components/Chat';
import Setup from '@/components/right_panel';
import { ChatOptions, ImageOptions } from '@/types/options';
import styles from './index.module.css';
import chatModelsService from '@/services/chat_models';
import { ChatRunResult } from '@/components/Chat/types';

type Props = {
  modelData: ChatModelData;
  modelType: ModelType;
};

type Params = {
  model_name: string;
};

const ChatModel = memo((props: Props) => {
  const { modelData, modelType } = props;
  // 路径参数
  const params = useParams<Params>();

  const [initialValues, setInitialValues] = useState<RightPanelValues>({
    initialChatConfig: {
      model: 'qwen-plus',
      temperature: 0.85,
      top_p: 0.8,
      seed: 1,
      enable_search: false,
      top_k: 0,
      stop: [],
      incremental_output: false,
      repetition_penalty: 1.1,
      tools: [],
    },
    initialImgConfig: {
      model: 'wanx-v1',
      responseFormat: '',
      n: 1,
      size: '1024*1024',
      style: '<auto>',
      seed: 0,
      ref_img: '',
      ref_strength: 0,
      ref_mode: '',
      negative_prompt: '',
    },
    initialTool: {},
  });
  const [modelOptions, setModelOptions] = useState<
    ChatOptions | ImageOptions
  >();
  const [prompt, setPrompt] = useState('');

  // 当 modelData.chatOptions 发生变化时同步更新 initialValues
  useEffect(() => {
    if (Object.keys(params).length === 0 || params.model_name != modelData.name) {
      return;
    }
    // 该属性不能传
    if (modelData != null && modelData.chatOptions != null) {
      delete modelData.chatOptions.proxyToolCalls;
    }
    setInitialValues((prev) => ({
      initialChatConfig: { ...modelData.chatOptions },
      initialImgConfig: { ...modelData.imageOptions },
      initialTool: {},
    }));
    if (modelData.modelType == ModelType.CHAT) {
      setModelOptions(modelData.chatOptions);
    } else if (modelData.modelType == ModelType.IMAGE) {
      setModelOptions(modelData.imageOptions);
    }
  }, [modelData]);

  const handleOptions = (options: ChatOptions | ImageOptions) => {
    setModelOptions(options);
  };

  const handlePrompt = (prompt: string) => {
    setPrompt(prompt);
  };

  return (
    modelData ? (
      <Flex justify="space-between" style={{ height: '100%' }}>
        <Chat
          modelData={modelData}
          modelType={modelType}
          modelOptions={modelOptions}
          onRun={async (param) => {
            let res: ChatModelRunResult | undefined;
            if (modelType === ModelType.CHAT) {
              res = await chatModelsService.postChatModel({
                input: param.input,
                chatOptions: modelOptions as ChatOptions,
                stream: param.stream,
                key: modelData.name,
                prompt: prompt,
              });
            } else if (modelType === ModelType.IMAGE) {
              res = await chatModelsService.postImageModel({
                input: param.input,
                imageOptions: modelOptions as ImageOptions,
                key: modelData.name,
                prompt: prompt,
              });
            }

            const ans = res
              ? (param.stream ? (res.result.streamResponse?.join('\n') as string) : (res.result.response as string))
              : '请求失败，请重试';

            return {
              result: ans,
              telemetry: {
                traceId: res ? res.telemetry.traceId : '',
              },
            } as ChatRunResult;
          }}
        />
        <Setup
          tabs={['config', 'prompt']}
          modelType={modelData.modelType}
          initialValues={initialValues}
          onChangeConfig={handleOptions}
          onChangePrompt={handlePrompt}
        />
      </Flex>
    ) : (
      <div className={styles['container']}>
        <Empty />
      </div>
    )
  );
});

export default ChatModel;
