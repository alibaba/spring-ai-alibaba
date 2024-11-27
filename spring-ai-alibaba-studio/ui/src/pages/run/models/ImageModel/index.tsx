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

import { useState } from 'react';
import { Card, Flex, Button, Checkbox, Input, Image } from 'antd';
import Setup from '../Setup';
import { ChatModelData, ChatModelResultData } from '@/types/chat_model';
import chatModelsService from '@/services/chat_models';

type ImageModelProps = {
  modelData: ChatModelData;
};

const ImageModel: React.FC<ImageModelProps> = ({ modelData }) => {
  const initialValues = {
    initialConfig: {
      model: 'ollama/llama3.2',
      temperature: 50,
      topP: 50,
      topK: 50,
      maxTokens: 10,
      sequences: '',
      version: 1,
    },
    initialTool: {},
  };

  const [inputValue, setInputValue] = useState('');
  const [imageValue, setImageValue] = useState('');

  const { TextArea } = Input;

  const runModel = async () => {
    try {
      const res = (await chatModelsService.postImageModel({
        input: inputValue,
        imageOptions: initialValues.initialConfig,
      })) as ChatModelResultData;
      console.log(res);
      setImageValue(res.result.response);
    } catch (error) {
      console.error('Failed to fetch chat models: ', error);
    }
  };

  const handleInputChange = (e) => {
    setInputValue(e.target.value);
  };

  return (
    <Flex justify="space-between">
      <Flex
        vertical
        justify="space-between"
        style={{ marginRight: 20, flexGrow: 1 }}
      >
        <div>
          <Card title={modelData.name}>
            <TextArea
              autoSize={{ minRows: 3 }}
              value={inputValue}
              onChange={handleInputChange}
            />
          </Card>
          <Card title="图片生成结果" style={{ marginTop: 20 }}>
            <Flex align="flex-end">
              <Image width={200} src={imageValue} />
              <Button style={{ marginLeft: 20 }}>下载</Button>
            </Flex>
          </Card>
        </div>
        <Flex align="center" justify="space-around">
          <Button>清空</Button>
          <Checkbox>聊天模式</Checkbox>
          <Button onClick={runModel}>运行</Button>
        </Flex>
      </Flex>
      <Setup initialValues={initialValues} />
    </Flex>
  );
};

export default ImageModel;
