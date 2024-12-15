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
import { Card, Flex, Button, Checkbox, Input } from 'antd';
import Setup from '../Setup';
import { ChatModelData, ChatModelResultData } from '@/types/chat_model';
import { ChatOptions } from '@/types/options';
import chatModelsService from '@/services/chat_models';
import { RightPanelValues } from '../types';

type ChatModelProps = {
  modelData: ChatModelData;
};

const ChatModel: React.FC<ChatModelProps> = ({ modelData }) => {
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
    initialTool: {},
  });
  const [prompt, setPrompt] = useState('');

  // 当 modelData.chatOptions 发生变化时同步更新 initialValues
  useEffect(() => {
    // delete modelData.chatOptions.proxyToolCalls;
    setInitialValues((prev) => ({
      initialChatConfig: { ...modelData.chatOptions },
      initialTool: {},
    }));
  }, [modelData.chatOptions]);

  const [inputValue, setInputValue] = useState('');
  const [isStream, setIsStream] = useState(false);

  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };

  const handleStremChange = (e) => {
    setIsStream(e.target.value);
  };

  const [messages, setMessages] = useState(
    [] as Array<{ type: string; content: string }>,
  );

  const handlePrompt = (prompt: string) => {
    setPrompt(prompt);
  };

  const runModel = async () => {
    try {
      const res = (await chatModelsService.postChatModel({
        input: inputValue,
        chatOptions: initialValues.initialChatConfig,
        stream: isStream,
        key: modelData.name,
        prompt: prompt,
      })) as ChatModelResultData;
      setMessages([
        ...messages,
        {
          type: 'user',
          content: inputValue,
        },
        {
          type: 'model',
          content: res.result.response,
        },
      ]);
      setInputValue('');
    } catch (error) {
      console.error('Failed to fetch chat models: ', error);
    }
  };

  const { TextArea } = Input;

  return (
    <Flex justify="space-between" style={{ height: '100%' }}>
      <Flex
        vertical
        justify="space-between"
        style={{ marginRight: 20, flexGrow: 1 }}
      >
        <div>
          <Flex vertical>
            {messages.map((message: any, index) => {
              return (
                <Card
                  key={index}
                  style={{
                    marginTop: 20,
                    marginLeft: message.type === 'user' ? 50 : 0,
                    marginRight: message.type === 'user' ? 0 : 50,
                  }}
                >
                  <p>{message.content}</p>
                </Card>
              );
            })}
          </Flex>
        </div>
        <Flex vertical>
          <TextArea
            autoSize={{ minRows: 3 }}
            style={{ marginBottom: 20 }}
            value={inputValue}
            onChange={handleInputChange}
          />
          <Flex style={{ flexDirection: 'row-reverse' }}>
            <Flex style={{ width: 300 }} align="center" justify="space-around">
              <Button>清空</Button>
              <Checkbox checked={isStream} onChange={handleStremChange}>
                聊天模式
              </Checkbox>
              <Button onClick={runModel}>运行</Button>
            </Flex>
          </Flex>
        </Flex>
      </Flex>
      <Setup initialValues={initialValues} onChangePrompt={handlePrompt} />
    </Flex>
  );
};

export default ChatModel;
