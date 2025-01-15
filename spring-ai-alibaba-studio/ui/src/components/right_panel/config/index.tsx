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

import {
  Form,
  Select,
  Slider,
  Input,
  Flex,
  InputNumber,
  Button,
  Tooltip,
  Switch,
} from 'antd';
import type { SelectProps } from 'antd';
import { ChatOptions, ImageOptions } from '@/types/options';
import { ModelType } from '@/types/chat_model';
import { useEffect, useState } from 'react';
import { LoadingOutlined } from '@ant-design/icons';
import chatModelsService from '@/services/chat_models';

type Props = {
  modelType: ModelType;
  configFromAPI: ChatOptions | ImageOptions;
  onChangeConfig: (cfg: ChatOptions | ImageOptions) => void;
};

const { Option } = Select;

export default function Config(props: Props) {
  const { modelType, configFromAPI, onChangeConfig } = props;
  const [form] = Form.useForm<ChatOptions | ImageOptions>();

  const initialChatConfig: ChatOptions = {
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
  };
  const initialImgConfig: ImageOptions = {
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
  };

  const [modelOptions, setModelOptions] = useState<SelectProps['options']>([]);

  const tipLabel = (left, desc) => {
    return (
      <Tooltip title={desc} placement="topLeft">
        <Flex justify="space-between" style={{ width: 300 }}>
          <span>{left}</span>
        </Flex>
      </Tooltip>
    );
  };

  const reset = () => {
    form.resetFields();
  };

  useEffect(() => {
    const fetchData = async () => {
      const modelNameList = await chatModelsService.getModelNames(modelType);
      if (modelNameList.length > 0) {
        setModelOptions(
          modelNameList.map((modelName) => ({
            value: modelName,
            label: modelName,
          })),
        );
      }
    };
    fetchData();
    form.setFieldsValue(configFromAPI);
  }, [configFromAPI]);

  return (
    <>
      <Form
        layout="vertical"
        form={form}
        initialValues={modelType == ModelType.CHAT ? initialChatConfig : initialImgConfig}
        onValuesChange={(changedValues, allValues) => {
          console.log(changedValues, allValues);
          onChangeConfig(allValues);
        }}
      >
        <Form.Item label="Model" name="model">
          <Select style={{ width: 200 }} options={modelOptions} />
        </Form.Item>
        {modelType == ModelType.CHAT ? (
          <>
            <Form.Item
              label={tipLabel(
                'Temperature',
                '用于控制随机性和多样性的程度。具体来说，temperature值控制了生成文本时对每个候选词的概率分布进行平滑的程度。较高的temperature值会降低概率分布的峰值，使得更多的低概率词被选择，生成结果更加多样化；而较低的temperature值则会增强概率分布的峰值，使得高概率词更容易被选择，生成结果更加确定。',
              )}
              name="temperature"
            >
              <Slider max={2.0} min={0.0} step={0.1} />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'seed',
                '生成时使用的随机数种子，用户控制模型生成内容的随机性。seed支持无符号64位整数。在使用seed时，模型将尽可能生成相同或相似的结果，但目前不保证每次生成的结果完全相同。',
              )}
              name="seed"
            >
              <InputNumber min={0} max={Number.MAX_SAFE_INTEGER} />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'Top P',
                '生成时，核采样方法的概率阈值。例如，取值为0.8时，仅保留累计概率之和大于等于0.8的概率分布中的token，作为随机采样的候选集。取值范围为（0,1.0)，取值越大，生成的随机性越高；取值越低，生成的随机性越低。默认值为0.8。',
              )}
              name="top_p"
            >
              <Slider max={1.0} min={0.0} step={0.1} />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'Top K',
                '生成时，采样候选集的大小。例如，取值为50时，仅将单次生成中得分最高的50个token组成随机采样的候选集。取值越大，生成的随机性越高；取值越小，生成的确定性越高。注意：如果top_k参数为空或者top_k的值大于100，表示不启用top_k策略，此时仅有top_p策略生效，默认是空。',
              )}
              name="top_k"
            >
              <Slider />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'enable_search',
                '模型内置了互联网搜索服务，该参数控制模型在生成文本时是否参考使用互联网搜索结果',
              )}
              name="enable_search"
              valuePropName="checked"
            >
              <Switch />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'repetition_penalty',
                '用于控制模型生成时的重复度。提高repetition_penalty时可以降低模型生成的重复度。1.0表示不做惩罚。默认为1.1。',
              )}
              name="repetition_penalty"
            >
              <InputNumber step={0.1} />
            </Form.Item>
          </>
        ) : modelType == ModelType.IMAGE ? (
          <>
            <Form.Item
              label={tipLabel('n', '要生成的图像数量。必须介于 1 和 4 之间')}
              name="n"
            >
              <InputNumber min={1} max={4} />
            </Form.Item>
            <Form.Item
              label={tipLabel('size_height', '输出图像的分辨率')}
              name="size"
            >
              <Select
                placeholder="输出图像的分辨率"
                allowClear
              >
                <Option value="1024*1024">1024*1024</Option>
                <Option value="720*1280">720*1280</Option>
                <Option value="768*1152">768*1152</Option>
                <Option value="1280*720">1280*720</Option>
              </Select>
            </Form.Item>
            <Form.Item label={tipLabel('style', '生成图片风格')} name="style">
              <Select placeholder="图片风格" allowClear>
                <Option value="<photography>">摄影</Option>
                <Option value="<portrait>">人像写真</Option>
                <Option value="<3d cartoon>">3D卡通</Option>
                <Option value="<anime>">动画</Option>
                <Option value="<oil painting>">油画</Option>
                <Option value="<watercolor>">水彩</Option>
                <Option value="<sketch>">素描</Option>
                <Option value="<chinese painting>">中国画</Option>
                <Option value="<flat illustration>">扁平插画</Option>
                <Option value="<auto>">默认值，由模型随机输出图像风格</Option>
              </Select>
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'seed',
                '随机数种子，用于控制模型生成内容的随机性',
              )}
              name="seed"
            >
              <InputNumber min={0} max={4294967290} />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'ref_img',
                '参考图像（垫图）的URL地址，模型根据参考图像生成相似风格的图像，支持jpg、png、tiff、webp',
              )}
              name="ref_img"
            >
              <Input />
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'ref_strength',
                '控制输出图像与垫图（参考图）的相似度。取值范围为[0.0, 1.0]。取值越大，代表生成的图像与参考图越相似。',
              )}
              name="ref_strength"
            >
              <Slider max={1.0} min={0.0} step={0.1} />
            </Form.Item>
            <Form.Item
              label={tipLabel('ref_mode', '基于垫图（参考图）生成图像的模式。')}
              name="ref_mode"
            >
              <Select
                placeholder="基于垫图（参考图）生成图像的模式"
                allowClear
              >
                <Option value="repaint">基于参考图的内容生成图像</Option>
                <Option value="refonly">基于参考图的风格生成图像</Option>
              </Select>
            </Form.Item>
            <Form.Item
              label={tipLabel(
                'negative_prompt',
                '反向提示词，用来描述不希望在画面中看到的内容，可以对画面进行限制。支持中英文，长度不超过500个字符，超过部分会自动截断。',
              )}
              name="negative_prompt"
            >
              <Input placeholder="反向提示词，对画面进行限制" />
            </Form.Item>
          </>
        ) : (
          <></>
        )}
      </Form>
      <Flex justify="center">
        <Button onClick={reset}>Reset</Button>
      </Flex>
    </>
  );
}
