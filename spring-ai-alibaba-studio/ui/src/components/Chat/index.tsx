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

import { useEffect, useState, useRef, memo } from 'react';
import {
  Flex,
  Card,
  Button,
  Checkbox,
  Input,
  Spin,
  Image,
  Divider,
} from 'antd';
import {
  ChatModelData,
  ChatModelResultData,
  ModelType,
} from '@/types/chat_model';
import chatModelsService from '@/services/chat_models';
import traceClients from '@/services/trace_clients';
import { RobotOutlined, UserOutlined, EyeOutlined } from '@ant-design/icons';
import styles from './index.module.css';
import { ChatOptions, ImageOptions } from '@/types/options';
import TraceDetailComp from '@/components/TraceDetailComp';
import { DataType } from '@/types/traces';

type Props = {
  modelData: ChatModelData;
  modelType: ModelType;
  modelOptions: ChatOptions | ImageOptions | undefined;
  prompt: string;
};

const ChatModel = memo((props: Props) => {
  const { modelData, modelType, modelOptions, prompt } = props;

  const [inputValue, setInputValue] = useState('');
  const [isStream, setIsStream] = useState(false);
  const [disabled, setDisabled] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [openTraceDetail, setOpenTraceDetail] = useState(false);
  const [traceDetail, setTraceDetail] = useState<DataType>({} as any);

  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };

  const handleStreamChange = (e) => {
    setIsStream(e.target.value);
  };

  const [messages, setMessages] = useState(
    [] as Array<{
      type: string;
      content: JSX.Element | string;
      isClear?: boolean;
      traceId?: string;
    }>,
  );

  const runModel = async () => {
    try {
      setDisabled(true);
      setInputValue('');
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

      let res: ChatModelResultData | undefined;
      if (modelType === ModelType.CHAT) {
        res = (await chatModelsService.postChatModel({
          input: inputValue,
          chatOptions: modelOptions,
          stream: isStream,
          key: modelData.name,
          prompt: prompt,
        })) as ChatModelResultData;
      } else if (modelType === ModelType.IMAGE) {
        res = (await chatModelsService.postImageModel({
          input: inputValue,
          imageOptions: modelOptions,
          key: modelData.name,
        })) as ChatModelResultData;
      }

      setMessages([
        ...messages,
        {
          type: 'user',
          content: inputValue,
        },
        {
          type: modelType === ModelType.CHAT ? 'chatModel' : 'imageModel',
          content: res ? res.result.response : '请求失败，请重试',
          traceId: res ? res.telemetry.traceId : '',
        },
      ]);
      setDisabled(false);
    } catch (error) {
      setDisabled(false);
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
    if (modelType === ModelType.CHAT) {
      setMessages([]);
    } else {
      setMessages((prevMessages) => {
        if (prevMessages.length === 0) return prevMessages;
        const updatedMessages = [...prevMessages];
        updatedMessages[updatedMessages.length - 1] = {
          ...updatedMessages[updatedMessages.length - 1],
          isClear: true,
        };
        return updatedMessages;
      });
    }
  };

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleTraceDetail = async (traceId) => {
    const res = (await traceClients.getTraceDetailClientById(
      traceId,
    )) as DataType;
    setTraceDetail(res);
    setOpenTraceDetail(true);
  };

  return (
    <>
      <Flex vertical style={{ marginRight: 20, flexGrow: 1, height: '100%' }}>
        <div className={styles['message-wrapper']}>
          {messages.map((message: any, index) => {
            return (
              <>
                <Flex
                  key={index}
                  className={styles['message']}
                  style={{
                    alignSelf: message.type === 'user' ? 'end' : 'auto',
                  }}
                  ref={
                    index === messages.length - 1 ? messagesEndRef : undefined
                  }
                >
                  {message.type !== 'user' && (
                    <RobotOutlined className={styles['message-icon']} />
                  )}
                  <Card
                    style={{
                      marginLeft: message.type === 'user' ? 0 : 10,
                      marginRight: message.type === 'user' ? 10 : 0,
                    }}
                  >
                    {message.type !== 'imageModel' && (
                      <div>{message.content}</div>
                    )}
                    {message.type === 'imageModel' && (
                      <Flex align="flex-end">
                        <Image width={200} src={message.content} />
                        <Button type="primary" style={{ marginLeft: 10 }}>
                          下载
                        </Button>
                      </Flex>
                    )}
                  </Card>
                  {message.type !== 'user' && (
                    <EyeOutlined
                      style={{ fontSize: 20, marginLeft: 10, alignSelf: 'end' }}
                      onClick={() => handleTraceDetail(message.traceId)}
                    />
                  )}
                  {message.type === 'user' && (
                    <UserOutlined className={styles['message-icon']} />
                  )}
                </Flex>
                {message.isClear && <Divider>上下文已结束</Divider>}
              </>
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
              <Checkbox checked={isStream} onChange={handleStreamChange}>
                流式响应
              </Checkbox>
              <Button onClick={runModel} disabled={disabled}>
                运行
              </Button>
            </Flex>
          </Flex>
        </Flex>
      </Flex>
      <TraceDetailComp
        record={traceDetail}
        open={openTraceDetail}
        setOpen={setOpenTraceDetail}
      />
    </>
  );
});

export default ChatModel;
