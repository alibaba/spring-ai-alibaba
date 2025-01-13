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
  message,
  Radio,
} from 'antd';
import {
  ChatModelData,
  ChatModelRunResult,
  ModelRunActionParam,
  ModelType,
} from '@/types/chat_model';
import traceClients from '@/services/trace_clients';
import { RobotOutlined, UserOutlined, EyeOutlined } from '@ant-design/icons';
import styles from './index.module.css';
import { ChatOptions, ImageOptions } from '@/types/options';
import TraceDetailComp from '@/components/trace_detail_comp';
import { TraceInfo } from '@/types/traces';
import { convertToTraceInfo } from '@/utils/trace_util';
import { ChatRunResult, ChatScene } from './types';
import { ClientRunActionParam } from '@/types/chat_clients';

type Props = {
  modelData: ChatModelData;
  modelType: ModelType;
  modelOptions: ChatOptions | ImageOptions | undefined;
  isMemoryEnabled?: boolean;
  callScene?: ChatScene; // 调用场景

  onRun: (param: ModelRunActionParam | ClientRunActionParam) => Promise<ChatRunResult>;
  onClear?: () => void; // 清楚历史上下文的回调
};

// 聊天组件
const ChatModel = memo((props: Props) => {
  const { modelType, isMemoryEnabled, callScene, onRun, onClear } = props;

  const [inputValue, setInputValue] = useState('');
  const [isStream, setIsStream] = useState(false);
  const [disabled, setDisabled] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [openTraceDetail, setOpenTraceDetail] = useState(false);
  const [traceDetail, setTraceDetail] = useState<TraceInfo>({} as any);

  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };

  const handleStreamChange = (e) => {
    setIsStream(e.target.checked);
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
      const res = await onRun({
        input: inputValue,
        stream: isStream,
      });

      const ans = res
        ? res.result
        : '请求失败，请重试';

      setMessages([
        ...messages,
        {
          type: 'user',
          content: inputValue,
        },
        {
          type: modelType === ModelType.CHAT ? 'chatModel' : 'imageModel',
          content: ans,
          traceId: res ? res.telemetry.traceId : '',
        },
      ]);
    } catch (error) {
      console.error('Failed to fetch chat models: ', error);
    } finally {
      setDisabled(false);
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
    if (callScene && callScene == ChatScene.CLIENT) {
      setMessages((prevMessages) => {
        if (prevMessages.length === 0) return prevMessages;
        const updatedMessages = [...prevMessages];
        updatedMessages[updatedMessages.length - 1] = {
          ...updatedMessages[updatedMessages.length - 1],
          isClear: true,
        };
        return updatedMessages;
      });
    } else {
      setMessages([]);
    }
    if (onClear) {
      onClear();
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
    try {
      const res = (await traceClients.getTraceDetailClientById(
        traceId,
      )) as TraceInfo;

      const traceInfo = convertToTraceInfo(res);
      if (traceInfo !== null) {
        setTraceDetail(traceInfo);
        setOpenTraceDetail(true);
      } else {
        message.error('TraceInfo is null');
      }
    } catch (error) {
      console.error('Failed to fetch trace detail: ', error);
    }
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
                  {message.type !== 'user' && message.traceId && (
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
          <Flex align="center" justify="space-between">
            <Flex >
              <Checkbox checked={isMemoryEnabled} onChange={() => { }}>对话记忆</Checkbox>
            </Flex>
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
