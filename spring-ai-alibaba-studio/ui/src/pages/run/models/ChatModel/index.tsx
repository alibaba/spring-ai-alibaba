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

import { useEffect, useState, useRef } from 'react';
import { Card, Flex, Button, Checkbox, Input, Spin, Image } from 'antd';
import Setup from '../Setup';
import { ChatModelData, ChatModelResultData } from '@/types/chat_model';
import chatModelsService from '@/services/chat_models';
import { RightPanelValues } from '../types';
import { RobotOutlined, UserOutlined } from '@ant-design/icons';
import styles from './index.module.css';

type ChatModelProps = {
  modelData: ChatModelData;
  modeType: 'CHAT' | 'IMAGE';
};

const ChatModel: React.FC<ChatModelProps> = ({ modelData, modeType }) => {
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

  // 当 modelData.chatOptions 发生变化时同步更新 initialValues
  useEffect(() => {
    delete modelData.chatOptions.proxyToolCalls;
    setInitialValues((prev) => ({
      initialChatConfig: { ...modelData.chatOptions },
      initialTool: {},
    }));
  }, [modelData.chatOptions]);

  const [inputValue, setInputValue] = useState('');
  const [isStream, setIsStream] = useState(false);
  const [disabled, setDisabled] = useState(false);
  const messagesEndRef = useRef(null);

  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };

  const handleStremChange = (e) => {
    setIsStream(e.target.value);
  };

  const [messages, setMessages] = useState(
    [] as Array<{ type: string; content: JSX.Element | string }>,
  );

  const runModel = async () => {
    try {
      setDisabled(true);
      setMessages([
        ...messages,
        {
          type: 'user',
          content: inputValue,
        },
        {
          type: 'model',
          content: loading(),
        },
      ]);

      if (modeType === 'CHAT') {
        const res = (await chatModelsService.postChatModel({
          input: inputValue,
          chatOptions: initialValues.initialChatConfig,
          stream: isStream,
          key: modelData.name,
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
      } else {
        const res = (await chatModelsService.postImageModel({
          input: inputValue,
          imageOptions: initialValues.initialChatConfig,
          key: modelData.name,
        })) as ChatModelResultData;

        setMessages([
          ...messages,
          {
            type: 'user',
            content: inputValue,
          },
          {
            type: 'model',
            content: (
              <Flex>
                <Image width={200} src={res.result.response} />
                <Button type="primary" style={{ marginLeft: 10 }}>
                  下载
                </Button>
              </Flex>
            ),
          },
        ]);
      }
      setDisabled(false);
      setInputValue('');
    } catch (error) {
      console.error('Failed to fetch chat models: ', error);
    }
  };

  const { TextArea } = Input;

  const loading = () => {
    return (
      <Spin tip="Loading">
        <div className={styles['message-loading']} />
      </Spin>
    );
  };

  const cleanHistory = () => {
    setMessages([]);
  };

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  return (
    <Flex justify="space-between" style={{ height: '100%' }}>
      <Flex vertical style={{ marginRight: 20, flexGrow: 1, height: '100%' }}>
        <div className={styles['message-wrapper']}>
          {messages.map((message: any, index) => {
            return (
              <Flex
                className={styles['message']}
                style={{
                  alignSelf: message.type === 'user' ? 'end' : 'auto',
                }}
                ref={index === messages.length - 1 ? messagesEndRef : undefined}
              >
                {message.type === 'model' && (
                  <RobotOutlined className={styles['message-icon']} />
                )}
                <Card
                  key={index}
                  style={{
                    marginLeft: message.type === 'user' ? 0 : 10,
                    marginRight: message.type === 'user' ? 10 : 0,
                  }}
                >
                  <p>{message.content}</p>
                </Card>
                {message.type === 'user' && (
                  <UserOutlined className={styles['message-icon']} />
                )}
              </Flex>
            );
          })}
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
              <Button onClick={cleanHistory}>清空</Button>
              <Checkbox checked={isStream} onChange={handleStremChange}>
                聊天模式
              </Checkbox>
              <Button onClick={runModel} disabled={disabled}>
                运行
              </Button>
            </Flex>
          </Flex>
        </Flex>
      </Flex>
      <Setup initialValues={initialValues} />
    </Flex>
  );
};

export default ChatModel;
