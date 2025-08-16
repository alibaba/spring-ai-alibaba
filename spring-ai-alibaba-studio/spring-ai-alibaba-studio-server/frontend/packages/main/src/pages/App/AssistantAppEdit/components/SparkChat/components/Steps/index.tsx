import $i18n from '@/i18n';
import {
  IFileSearchResult,
  IFileSearchResultFunction,
  IToolCall,
  IToolCallFunction,
} from '@/types/chat';
import { Accordion, AccordionProps } from '@spark-ai/chat';
import { IconFont, parseJsonSafely } from '@spark-ai/design';
import Knowledge from './Knowledge';
import Plugin from './Plugin';
import Reasoning from './Reasoning';

const StepsMsgStatus = {
  finished: $i18n.get({
    id: 'main.components.SparkChat.components.Steps.index.completed',
    dm: '执行完成',
  }),
  generating: $i18n.get({
    id: 'main.components.SparkChat.components.Steps.index.inProgress',
    dm: '执行中',
  }),
  interrupted: $i18n.get({
    id: 'main.components.SparkChat.components.Steps.index.stopped',
    dm: '已停止',
  }),
  error: $i18n.get({
    id: 'main.components.SparkChat.components.Steps.index.error',
    dm: '错误',
  }),
};
interface IProps {
  data: {
    content: IToolCall[];
    reasoning_content: string;
    msgStatus: 'finished' | 'generating' | 'interrupted';
  };
}
export default (props: IProps) => {
  const { data } = props;
  if (!data.content?.length && !data.reasoning_content?.length) {
    return null;
  }

  const mergeToolCalls = (content: IToolCall[]): AccordionProps[] => {
    /** merge the tool call messages according to the messages */
    const res: AccordionProps[] = [];
    if (!content?.length) {
      return res;
    }
    // knowledge base
    content.forEach((msg, msgIndex) => {
      if (msg.type === 'file_search_result') {
        // file search
        res.push({
          icon: <IconFont type="spark-document-line" size="small"></IconFont>,
          title: $i18n.get({
            id: 'main.components.SparkChat.components.Steps.index.knowledgeRetrieval',
            dm: '知识库检索',
          }),
          children: (
            <Knowledge
              key={`${msg.type}-${msg.id}`}
              data={
                parseJsonSafely(
                  (msg.function as IFileSearchResultFunction).output,
                ) as IFileSearchResult[]
              }
            ></Knowledge>
          ),
        });
      }
      if (msg.type === 'tool_call') {
        // plugin
        const matchedMsg = content.find(
          (otherMsg, idx) =>
            idx > msgIndex &&
            otherMsg.type === 'tool_result' &&
            otherMsg.id === msg.id,
        );
        res.push({
          icon: <IconFont type="spark-plugin-line" size="small"></IconFont>,
          title: $i18n.get(
            {
              id: 'main.components.SparkChat.components.Steps.index.plugin',
              dm: '插件：{var1}',
            },
            { var1: (msg.function as IToolCallFunction)?.name },
          ),
          children: (
            <Plugin
              params={{
                arguments: (msg.function as IToolCallFunction)?.arguments || '',
                output:
                  (matchedMsg?.function as IToolCallFunction)?.output || '',
              }}
            ></Plugin>
          ),
        });
      }
      if (msg.type === 'mcp_tool_call') {
        // mcp
        const matchedMsg = content.find(
          (otherMsg, idx) =>
            idx > msgIndex &&
            otherMsg.type === 'mcp_tool_result' &&
            otherMsg.id === msg.id,
        );
        res.push({
          icon: <IconFont type="spark-MCP-mcp-line" size="small"></IconFont>,
          title: $i18n.get(
            {
              id: 'main.pages.App.AssistantAppEdit.components.SparkChat.components.Steps.index.mcp',
              dm: 'MCP：{var1}',
            },
            { var1: (msg.function as IToolCallFunction).name },
          ),
          children: (
            <Plugin
              params={{
                arguments: (msg.function as IToolCallFunction)?.arguments,
                output: (matchedMsg?.function as IToolCallFunction)?.output,
              }}
            ></Plugin>
          ),
        });
      }
      if (msg.type === 'component_tool_call') {
        // component(workflow or agent)
        const matchedMsg = content.find(
          (otherMsg, idx) =>
            idx > msgIndex &&
            otherMsg.type === 'component_tool_result' &&
            otherMsg.id === msg.id,
        );
        res.push({
          icon: <IconFont type="spark-doubleStar-line" size="small"></IconFont>,
          title: $i18n.get(
            {
              id: 'main.pages.App.AssistantAppEdit.components.SparkChat.components.Steps.component',
              dm: '组件：{var1}',
            },
            {
              var1: (msg.function as IToolCallFunction)?.name,
            },
          ),
          children: (
            <Plugin
              params={{
                arguments: (msg.function as IToolCallFunction)?.arguments,
                output: (matchedMsg?.function as IToolCallFunction)?.output,
              }}
            ></Plugin>
          ),
        });
      }
    });
    return res;
  };

  const getSteps = () => {
    const steps = mergeToolCalls(data.content);
    if (data.reasoning_content?.length) {
      steps.unshift({
        icon: <IconFont type="spark-deepThink-line" size="small"></IconFont>,
        title: $i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.SparkChat.components.Steps.index.deepThinking',
          dm: '深度思考',
        }),
        children: <Reasoning reasoning={data.reasoning_content}></Reasoning>,
      });
    }
    return steps;
  };

  return (
    <Accordion
      title={StepsMsgStatus[data.msgStatus]}
      status={data.msgStatus}
      steps={getSteps()}
      defaultOpen={true}
    ></Accordion>
  );
};
