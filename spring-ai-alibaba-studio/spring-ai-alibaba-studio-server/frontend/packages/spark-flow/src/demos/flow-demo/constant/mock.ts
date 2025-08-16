import $i18n from '@/i18n';
import { IModelParamsSchema, IModelSelectorItem } from '../types/model';

export const mockModelList: IModelSelectorItem[] = [
  {
    provider: {
      provider: 'openai',
      name: 'OpenAI',
      description: $i18n.get({
        id: 'spark-flow.demos.spark-flow-1.constant.mock.openAiProvider',
        dm: 'OpenAI提供商',
      }),
      icon: 'https://example.com/openai.png',
      source: 'preset',
      enable: true,
      supported_model_types: ['chat', 'embedding'],
    },
    models: [
      {
        model_id: 'gpt-4',
        name: 'GPT-4',
        provider: 'openai',
        mode: 'chat',
        type: 'llm',
        tags: [
          'AI',
          'Chat',
          $i18n.get({
            id: 'spark-flow.demos.spark-flow-1.constant.mock.largeLanguageModel',
            dm: '大语言模型',
          }),
          'vision',
        ],
        icon: 'https://example.com/gpt4.png',
      },
      {
        model_id: 'gpt-3.5-turbo',
        name: 'GPT-3.5 Turbo',
        provider: 'openai',
        mode: 'chat',
        type: 'llm',
        tags: [
          'AI',
          'Chat',
          $i18n.get({
            id: 'spark-flow.demos.spark-flow-1.constant.mock.largeLanguageModel',
            dm: '大语言模型',
          }),
        ],
        icon: 'https://example.com/gpt35.png',
      },
    ],
  },
];

export const mockModelParamsSchema: IModelParamsSchema[] = [
  {
    key: 'temperature',
    name: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.mock.temperature',
      dm: '温度',
    }),
    description: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.mock.controlModelOutputRandomness',
      dm: '控制模型输出的随机性',
    }),
    type: 'Number',
    default_value: 0.7,
    min: 0,
    max: 2,
    precision: 2,
    required: false,
  },
  {
    key: 'max_tokens',
    name: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.mock.maxTokens',
      dm: '最大令牌数',
    }),
    description: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.mock.maximumLengthOfGeneratedText',
      dm: '生成文本的最大长度',
    }),
    type: 'Number',
    default_value: 1000,
    min: 1,
    max: 4096,
    precision: 0,
    required: false,
  },
];
