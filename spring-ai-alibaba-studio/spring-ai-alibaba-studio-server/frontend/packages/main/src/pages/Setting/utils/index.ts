import $i18n from '@/i18n';

export const BAILIAN_URL = 'https://bailian.console.aliyun.com/';

export const API_KEY_TIP_SECTIONS = [
  {
    title: $i18n.get({
      id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.modelServiceProvider',
      dm: '模型服务供应商',
    }),
    linkButtons: [
      {
        text: $i18n.get({
          id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.AliyunBailian',
          dm: '阿里云百炼',
        }),
        url: 'https://bailian.console.aliyun.com/console?tab=home#/home',
      },
      {
        text: 'ModelStudio',
        url: 'https://www.alibabacloud.com/en/product/modelstudio?_p_lc=1',
      },
      { text: 'OpenAI', url: 'https://openai.com/api/' },
      { text: 'OpenRouter', url: 'https://openrouter.ai/' },
    ],
    description: $i18n.get({
      id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.providerNameDescription',
      dm: '您可以选择符合OpenAI API格式的模型服务供应商，注册API Key及API调用地址，如阿里云百炼（中国大陆）、ModelStudio（新加坡）、OpenAI、OpenRouter、硅基智能及其他同类型厂商。',
    }),
  },
];
