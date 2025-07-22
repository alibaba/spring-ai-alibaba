import { IReceiveMessage, IUsage } from '@/types/chat';
import { TMessage } from '@spark-ai/chat';

export const convertAgentMsgToSparkChat = (
  agentMsg: IReceiveMessage,
): Omit<TMessage, 'cards'> & {
  cards: NonNullable<TMessage['cards']>;
  usage?: IUsage;
} => {
  return {
    id: agentMsg.request_id,
    cards: [
      {
        code: 'Steps',
        data: {
          content: agentMsg.message?.tool_calls || [],
          reasoning_content: agentMsg.message?.reasoning_content,
          msgStatus: agentMsg.message?.content?.length
            ? 'finished'
            : 'generating',
          defaultOpen: true,
        },
      },
      {
        code: 'Text',
        data: {
          content: agentMsg.message?.content,
          msgStatus: 'generating',
        },
      },
    ],
    content: '',
    role: 'assistant',
    msgStatus: 'generating',
    usage: agentMsg.usage,
  };
};
