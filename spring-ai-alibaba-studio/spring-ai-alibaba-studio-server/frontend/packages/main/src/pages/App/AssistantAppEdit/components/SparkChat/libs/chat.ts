import EventEmitter from 'eventemitter3';
import json5 from 'json5';
// @ts-ignore
import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { BizVars } from '../../VarConfigDrawer';
import { Rpc } from './rpc';

type IReceiveMessage = any;

interface IChatOpts {
  conversation_id?: string; // conversation id
  configParams?: any; // parameters except content, including custom variables, plugin variables, etc.
  type?: 'experience' | 'dialog'; // dialog type, usage is questionable
  draft?: boolean; // whether it is a draft
}

interface IMessage {
  role: 'user' | 'assistant';
  content: string | Record<string, any>;
  content_type: string;
}
export class Chat extends EventEmitter {
  private conversation_id: string;
  private configParams: { bizVars: BizVars };
  private countTime: number = 0;
  private timer: any;
  rpc: Rpc;
  public draft: boolean;
  public cacheMessage: IReceiveMessage | null = null;
  private messages: IMessage[] = []; // history message list

  constructor(opts: IChatOpts) {
    super();
    this.conversation_id = opts.conversation_id || ''; // record the conversation id when the first message is returned
    this.configParams = opts.configParams || {};
    this.draft = opts.draft || false;
    this.rpc = new Rpc();
  }

  destroy = () => {
    this.rpc.destroy();
  };

  changeConfigParams = (payload: any) => {
    this.configParams = {
      ...this.configParams,
      ...payload,
    };
  };

  changeDraft = (val: boolean) => {
    this.draft = val;
  };

  updateConversationId(value: string) {
    this.conversation_id = value;
  }
  resetSession = () => {
    this.messages = [];
    this.cacheMessage = null;
  };

  normalGenerate(
    value: string,
    options: { imageList?: string[]; app_id?: string } = {},
  ) {
    if (!options.imageList?.length) {
      this.messages.push({
        role: 'user',
        content: value,
        content_type: 'text',
      });
    } else {
      const newMessage = [
        {
          type: 'text',
          text: value,
        },
        ...options.imageList?.map((item) => ({
          type: 'image',
          path: item,
        })),
      ];

      this.messages.push({
        role: 'user',
        content: newMessage,
        content_type: 'multimodal',
      });
    }
    return this.sendMessage({
      app_id: options.app_id,
      messages: this.messages,
      stream: true,
      timeout: defaultSettings.agentSSETimeout,
    });
  }

  close() {
    // manually close
    this.messages.push({
      role: 'assistant',
      content: this.cacheMessage?.message?.content,
      content_type: this.cacheMessage?.message?.content_type || 'text',
    });
    this.cacheMessage = null;
    this.rpc.close();
  }

  private sendMessage(params: any) {
    clearInterval(this.timer);
    return (this.rpc as Rpc).conversation(
      {
        ...params,
        extra_params: this.configParams.bizVars.user_defined_params,
        prompt_variables: this.configParams.bizVars.prompt_variables,
        is_draft: this.draft,
      },
      {
        onopen: () => {
          this.cacheMessage = null;
          this.timer = setInterval(() => {
            this.countTime++;
            if (this.countTime === params.timeout) {
              clearInterval(this.timer);
              this.rpc.control?.abort();
              this.countTime = 0;
            }
          }, 1000);
        },
        onmessage: (event: any) => {
          clearInterval(this.timer);
          const { data } = event;
          let parsed: IReceiveMessage;
          try {
            parsed = json5.parse(data);
            if (parsed.conversation_id) {
              this.conversation_id = parsed.conversation_id;
            }
            if (!parsed.request_id || (parsed.error && !parsed.content)) {
              // error thrown by the server in the sse stream
              clearInterval(this.timer);
              console.log('[sse]error', parsed);
              this.emit(
                'error',
                parsed.error?.errorMsg ||
                  parsed.error?.message ||
                  $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.libs.chat.unknownError',
                    dm: '未知错误',
                  }),
              );
            } else {
              // normal case
              if (!this.cacheMessage) {
                this.cacheMessage = parsed;
                if (!this.cacheMessage?.message) {
                  this.cacheMessage.message = {};
                }
              } else {
                this.cacheMessage.usage = parsed.usage;
                // parse the deep thinking
                if (parsed.message.reasoning_content?.length) {
                  // merge the message
                  this.cacheMessage.message.reasoning_content =
                    (this.cacheMessage.message.reasoning_content || '') +
                    (parsed.message.reasoning_content || '');
                }
                // parse the content
                if (parsed.message.content?.length) {
                  // merge the message
                  this.cacheMessage.message.content =
                    (this.cacheMessage.message.content || '') +
                    (parsed.message.content || '');
                }
                // merge the tool calls
                if (parsed.message.tool_calls?.length) {
                  this.cacheMessage.message.tool_calls = [
                    ...(this.cacheMessage.message.tool_calls || []),
                    ...parsed.message.tool_calls,
                  ];
                }
              }
              const concatedMessage = {
                ...parsed,
                ...this.cacheMessage,
              };
              this.cacheMessage = concatedMessage;
              this.emit('message', concatedMessage);
            }
          } catch (error) {
            // json parse error
            this.emit(
              'error',
              $i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.SparkChat.libs.chat.messageParseError',
                dm: '消息解析错误',
              }),
              event.data,
            );
            console.error(error, event.data);
            return;
          }
        },
        onclose: () => {
          console.log('[sse]close');
          this.messages.push({
            role: 'assistant',
            content: this.cacheMessage?.message?.content,
            content_type: this.cacheMessage?.message?.content_type || 'text',
          });
          this.emit('close', this.cacheMessage);
          this.cacheMessage = null;
        },
        onerror: (err: any) => {
          // error of the sse link itself
          clearInterval(this.timer);
          console.log('[sse]error', err);
          this.cacheMessage = null;
          this.emit('error', err?.errorMsg || err);
        },
      },
    );
  }
}

export default Chat;
