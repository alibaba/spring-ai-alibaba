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
import { Card, Flex, Button, Checkbox, Input, Image } from 'antd';
import Setup from '../Setup';
import { ChatModelData, ChatModelResultData } from '@/types/chat_model';
import chatModelsService from '@/services/chat_models';
import { RightPanelValues } from '../types';
import { ChatOptions, ImageOptions } from '@/types/options';
import { ModelType } from '@/types/chat_model';


type ImageModelProps = {
  modelData: ChatModelData;
};

const ImageModel: React.FC<ImageModelProps> = ({ modelData }) => {
  const [initialValues, setInitialValues] = useState<RightPanelValues>({
    initialImgConfig: {
      model: 'wanx-v1',
      responseFormat: '',
      n: 1,
      size: '',
      style: '',
      seed: 0,
      ref_img: '',
      ref_strength: 0,
      ref_mode: '',
      negative_prompt: '',
    },
    initialTool: {},
  });
  const [prompt, setPrompt] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [imageValue, setImageValue] = useState('');
  const [modelOptions, setModelOptions] = useState<ImageOptions>();

  const { TextArea } = Input;

  // 当 modelData.chatOptions 发生变化时同步更新 initialValues
  useEffect(() => {
    // 该属性不能传
    // delete modelData.chatOptions.proxyToolCalls;
    setInitialValues((prev) => ({
      initialImgConfig: { ...modelData.imageOptions },
      initialTool: {},
    }));
  }, [modelData.chatOptions]);

  const runModel = async () => {
    try {
      const res = (await chatModelsService.postImageModel({
        input: inputValue,
        imageOptions: modelOptions,
        key: modelData.name,
        prompt: prompt,
      })) as ChatModelResultData;
      setImageValue(res.result.response);
    } catch (error) {
      console.error('Failed to fetch chat models: ', error);
    }
  };

  const handleOptions = (options: ImageOptions) => {
    setModelOptions(options);
  };

  const handlePrompt = (prompt: string) => {
    setPrompt(prompt);
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
      <Setup modelType={ModelType.IMAGE} initialValues={initialValues} onChangeConfig={handleOptions} onChangePrompt={handlePrompt} />
    </Flex>
  );
};

export default ImageModel;

