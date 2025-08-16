import $i18n from '@/i18n';
import { IRadioItemProps } from '../components/RadioItem';

export const installTypeOptions: IRadioItemProps[] = [
  {
    label: 'SSE',
    value: 'SSE',
    logo: 'spark-internet-line',
  },
];

export const MCP_TIP_SECTIONS = [
  {
    title: $i18n.get({
      id: 'main.pages.MCP.Create.mcpMarket',
      dm: 'MCP市场',
    }),
    linkButtons: [
      { text: 'ModelScope MCP', url: 'https://modelscope.cn/mcp' },
      { text: 'MCP.so', url: 'https://mcp.so' },
      { text: 'Simthery', url: 'https://smithery.ai/' },
    ],
    description: $i18n.get({
      id: 'main.pages.MCP.Create.mcpMarketDescription',
      dm: '在ModelScope MCP、MCP.so、Simthery等主流的MCP市场获取服务，将Sever地址配置在下方，即可完成自定义MCP服务的注册！',
    }),
  },
  {
    title: 'Nacos',
    linkButtons: [
      {
        text: 'Nacos MCP Registry',
        url: 'https://nacos.io/blog/nacos-gvr7dx_awbbpb_gg16sv97bgirkixe/?spm=5238cd80.2ef5001f.0.0.3f613b7caSfxcr&source=blog',
      },
    ],
    description: $i18n.get({
      id: 'main.pages.MCP.Create.nacosDescription',
      dm: '借助Nacos MCP Rourter结合Nacos MCP Registry，根据任务详情帮您自动推荐、安装、代理MCP Server，免去繁杂的配置与重启操作。\n请在页面下方输入Nacos MCP Router的配置，如果您不了解具体操作步骤，可前往：Nacos MCP Registry。',
    }),
  },
  {
    title: 'Higress',
    linkButtons: [
      {
        text: 'Higress Open API To MCP',
        url: 'https://higress.ai/blog/bulk-conversion-of-existing-openapi-to-mcp-server',
      },
    ],
    description: $i18n.get({
      id: 'main.pages.MCP.Create.higressDescription',
      dm: '可以借助 Higress 网关将内部署的业务应用、API平台接入百炼智能体，遵循 Higress 的 MCP 配置流程，可实现零代码改造将现有 API 转化为 MCP 服务（当前支持 HTTP、Dubbo 协议）。请在页面下方输入通过 Higress 代理的 MCP Server 地址，如果您不了解具体操作步骤，可前往：Higress Open API To MCP。',
    }),
  },
];
