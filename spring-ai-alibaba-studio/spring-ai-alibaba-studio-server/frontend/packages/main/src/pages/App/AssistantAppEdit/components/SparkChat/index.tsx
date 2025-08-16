import { AssistantAppContext } from '@/pages/App/AssistantAppEdit/AssistantAppContext';
import {
  ChatAnywhere,
  ChatAnywhereRef,
  createCard,
  DefaultCards,
  TMessage,
  uuid,
} from '@spark-ai/chat';
import {
  Button,
  copy,
  IconButton,
  IconFont,
  message,
  notification,
  Tooltip,
} from '@spark-ai/design';

import $i18n from '@/i18n';
import upload, { getPreviewUrl } from '@/request/upload';
import { IReceiveMessage } from '@/types/chat';
import { UploadFile } from 'antd';
import {
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import Steps from './components/Steps';
import Welcome from './components/Welcome';
import { convertAgentMsgToSparkChat } from './converter';
import Chat from './libs/chat';

interface IProps {
  maxTokenContext?: number;
  isBizVarsComplete: boolean;
  openVarDrawer: () => void;
}
export interface ISparkChatRef {
  onClear: () => void;
  resetSession: (disabledShowTip?: boolean) => void;
  chat: Chat | null;
}
export default forwardRef<ISparkChatRef, IProps>((props, ref) => {
  const { appCode, appState, setAppState } = useContext(AssistantAppContext);
  const { appBasicConfig, flushing } = appState;
  const sparkChatRef = useRef<ChatAnywhereRef>();
  const chatRef = useRef<Chat | null>(null);
  const hasFirstChatRef = useRef(false); // clear the welcome card when the first message is sent

  // if the bizVar is not complete, trigger the notification
  const [hasTriggeredNotification, setHasTriggeredNotification] =
    useState(false);
  const [api, contextHolder] = notification.useNotification();
  const triggerNotification = useCallback(() => {
    if (hasTriggeredNotification) return;
    setHasTriggeredNotification(true);
    api.info({
      message: $i18n.get({
        id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.tip',
        dm: '提示',
      }),
      description: (
        <div>
          <div>
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.configVariables',
              dm: '您还未进行入参变量配置，可能会影响智能体效果，建议您进行完善！',
            })}
          </div>
          <Button
            type="link"
            onClick={() => {
              props.openVarDrawer();
              api.destroy();
            }}
            className="float-right"
          >
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.configureNow',
              dm: '立即配置',
            })}
          </Button>
        </div>
      ),

      duration: 0,
    });
  }, [hasTriggeredNotification]);

  // cache the current query, answer
  const currentQA = useRef<{
    query?: TMessage;
    answer?: TMessage;
  }>({});

  const onInput = useCallback(
    async (
      data: { query: string; fileList?: UploadFile<any>[][] },
      isRegenerate = false,
    ) => {
      // if the custom variable is not complete, trigger the notification
      if (!props.isBizVarsComplete) {
        triggerNotification();
      }
      if (!hasFirstChatRef.current) {
        // clear the welcome card when the first message is sent
        sparkChatRef.current?.removeAllMessages();
        hasFirstChatRef.current = true;
      }
      // clear the regenerate button of previous answer
      if (currentQA.current.answer) {
        currentQA.current.answer.cards = currentQA.current.answer.cards?.map(
          (item) => {
            if (item.code === 'Footer') {
              return createFooterFromSparkChatMessage(
                currentQA.current.query?.id || '',
                currentQA.current.answer!,
                false,
              );
            }
            return item;
          },
        );
        sparkChatRef.current?.updateMessage(currentQA.current.answer);
      }
      if (!isRegenerate) {
        // regenerate the query, so the answer should be cleared
        currentQA.current.answer = undefined;
        // set the new query
        currentQA.current.query = {
          id: uuid(),
          cards: [],
          content: data.query,
          role: 'user',
          msgStatus: 'finished',
        };

        // if there are uploaded files, construct the related file cards
        if (data.fileList?.length) {
          const files = data.fileList[0];
          const images = data.fileList[1];
          if (images?.length) {
            currentQA.current.query.cards?.push(
              createCard(
                'Images',
                images.map((item) => {
                  return {
                    url: item.response.url,
                    originalFileData: item,
                  };
                }),
              ),
            );
          }

          if (files?.length) {
            currentQA.current.query.cards?.push(
              createCard(
                'Files',
                files.map((item) => {
                  return {
                    filename: item.name,
                    bytes: item.size,
                    originalFileData: item,
                  };
                }),
              ),
            );
          }
        }
        sparkChatRef.current?.updateMessage(currentQA.current.query);
      }

      // set the chat loading
      sparkChatRef.current?.setLoading(true);

      // set the new answer
      currentQA.current.answer = {
        id: uuid(),
        content: '',
        role: 'assistant',
        msgStatus: 'generating',
      };
      const imageList = data.fileList?.[0] || [];

      chatRef.current?.normalGenerate(data?.query, {
        imageList: imageList.length
          ? imageList.map((item) => item.response?.downloadUrl)
          : void 0,
        app_id: appCode,
      });

      setAppState({
        flushing: true,
      });
    },
    [triggerNotification, props.isBizVarsComplete],
  );

  const onStop = useCallback(() => {
    chatRef.current?.close(); // close the sse link
    if (currentQA.current.answer) {
      currentQA.current.answer.msgStatus = 'interrupted';
      currentQA.current.answer.cards = currentQA.current.answer.cards?.map(
        (item) => {
          return {
            ...item,
            data: {
              ...item.data,
              msgStatus:
                item.data.msgStatus === 'generating'
                  ? 'interrupted'
                  : item.data.msgStatus, // interrupt the card that is executing
            },
          };
        },
      );
      currentQA.current.answer.cards?.push(
        createFooterFromSparkChatMessage(
          chatRef.current?.cacheMessage?.request_id,
          currentQA.current.answer,
          true,
        ),
      );
      sparkChatRef.current?.updateMessage(currentQA.current.answer);
    }
    sparkChatRef.current?.setLoading(false); // set the chat container loading state to false
    setAppState({
      flushing: false,
    });
  }, []);

  const onRegenerate = useCallback(
    (msg: Partial<TMessage>) => {
      sparkChatRef.current?.removeMessage(msg); // delete the answer card
      const messages = sparkChatRef.current?.getMessages();
      if (!messages) return;
      const lastQuestion = messages[messages.length - 1];
      const imageList =
        lastQuestion.cards
          ?.find((card) => card.code === 'Images')
          ?.data?.map((d: any) => d.originalFileData) || [];
      const fileList =
        lastQuestion.cards
          ?.find((card) => card.code === 'Files')
          ?.data?.map((d: any) => d.originalFileData) || [];

      currentQA.current.answer = undefined;
      onInput(
        {
          query: messages[messages.length - 1]?.content!,
          fileList: [fileList, imageList],
        },
        true,
      );
    },
    [onInput],
  );

  const createFooterFromSparkChatMessage = (
    requestId: string,
    sparkChatMessage: TMessage,
    isLatestMsg = true,
  ) => {
    return createCard('Footer', {
      left: (
        <DefaultCards.FooterActions
          data={[
            {
              icon: (
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.copy',
                    dm: '复制',
                  })}
                  trigger={'hover'}
                >
                  <IconFont type="spark-copy-line" size="small"></IconFont>
                </Tooltip>
              ),

              label: '',
              onClick: () => {
                copy(
                  sparkChatMessage.cards?.find((item) => item.code === 'Text')
                    ?.data?.content,
                );
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.copySuccess',
                    dm: '复制成功',
                  }),
                );
              },
            },
            isLatestMsg
              ? {
                  icon: (
                    <Tooltip
                      title={$i18n.get({
                        id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.regenerate',
                        dm: '重新生成',
                      })}
                      trigger={'hover'}
                    >
                      <IconFont
                        type="spark-replace-line"
                        size="small"
                      ></IconFont>
                    </Tooltip>
                  ),

                  label: '',
                  onClick: () => {
                    const msg = sparkChatRef.current?.getMessage(
                      sparkChatMessage.id,
                    );
                    if (msg) {
                      onRegenerate(msg);
                    }
                  },
                }
              : undefined,
            {
              icon: (
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.clickCopyRequestId',
                    dm: '点击复制Request ID',
                  })}
                  trigger={'hover'}
                >
                  <IconFont type="spark-ID-line" size="small"></IconFont>
                </Tooltip>
              ),

              label: '',
              onClick: () => {
                copy(requestId);
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.copySuccess',
                    dm: '复制成功',
                  }),
                );
              },
            },
          ].filter((item) => !!item)}
        />
      ),

      right: (
        <DefaultCards.FooterCount
          data={[
            [
              $i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.wordCount',
                dm: '字数',
              }),

              sparkChatMessage.cards?.find((item) => item.code === 'Text')?.data
                ?.content?.length,
            ],

            [
              $i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.inputToken',
                dm: '输入token',
              }),
              // @ts-ignore
              sparkChatMessage.usage?.prompt_tokens,
            ],
            [
              $i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.outputToken',
                dm: '输出token',
              }),
              // @ts-ignore
              sparkChatMessage.usage?.completion_tokens,
            ],
          ]}
        />
      ),
    });
  };

  const onClear = useCallback(() => {
    // clear all the conversation
    chatRef.current?.resetSession();
    sparkChatRef.current?.removeAllMessages();
    currentQA.current.answer = undefined;
  }, []);

  const resetSession = useCallback(
    (disabledShowTip = false) => {
      if (flushing) {
        message.warning(
          $i18n.get({
            id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.generatingDialog',
            dm: '对话正在生成中，请先停止再重置对话！',
          }),
        );
        return;
      }
      onClear();
      chatRef.current?.updateConversationId('');
      // reset hasFirstChatRef, so the welcome card will be displayed again
      hasFirstChatRef.current = false;
      if (!disabledShowTip) {
        message.success(
          $i18n.get({
            id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.resetSuccess',
            dm: '重置对话成功，请继续测试',
          }),
        );
      }
    },
    [flushing, appBasicConfig],
  );

  useEffect(() => {
    if (chatRef.current || !appCode?.length) return;
    chatRef.current = new Chat({
      type: 'dialog',
      draft: true,
      configParams: {
        appId: appCode,
        bizVars: {},
      },
    });
    chatRef.current.on('message', (agentMsg: IReceiveMessage) => {
      const converted = convertAgentMsgToSparkChat(agentMsg);
      currentQA.current.answer = converted;
      sparkChatRef.current?.updateMessage(currentQA.current.answer);
    });
    chatRef.current.on('close', (cacheMessage: IReceiveMessage) => {
      setAppState({
        flushing: false,
      });
      // SSE stream ends
      if (currentQA.current.answer) {
        currentQA.current.answer.cards = currentQA.current.answer.cards?.map(
          (item) => {
            if (item.code === 'Text') {
              return {
                ...item,
                data: {
                  ...item.data,
                  msgStatus: 'finished',
                },
              };
            }
            return item;
          },
        );
        if (cacheMessage) {
          const converted = convertAgentMsgToSparkChat(cacheMessage);
          currentQA.current.answer.cards?.push(
            createFooterFromSparkChatMessage(
              cacheMessage?.request_id,
              converted,
              true,
            ),
          );
        }
        sparkChatRef.current?.updateMessage(currentQA.current.answer);
      }
      sparkChatRef.current?.setLoading(false);
    });
    chatRef.current.on('error', (err) => {
      setAppState({
        flushing: false,
      });
      if (currentQA.current.answer) {
        // clear all the cards in the answer, and construct the error card
        currentQA.current.answer.cards = [
          createCard('Text', {
            content: '',
            typing: true,
            msgStatus: 'generating',
          }),
          createCard('Interrupted', {
            type: 'error',
            title: 'Error',
            desc: String(err),
          }),
        ];
        sparkChatRef.current?.updateMessage(currentQA.current.answer);
      }
      sparkChatRef.current?.setLoading(false);
    });
    return () => {
      chatRef.current?.destroy();
      chatRef.current = null;
    };
  }, [appCode]);

  useEffect(() => {
    if (appState.canChat) {
      sparkChatRef.current?.setDisabled(false);
    } else {
      sparkChatRef.current?.setDisabled(true);
    }
  }, [appState.canChat]);

  useEffect(() => {
    return () => {
      onClear();
    };
  }, []);

  useEffect(() => {
    chatRef.current?.changeDraft(!appState.isReleaseVersion);
  }, [appState.isReleaseVersion]);

  useEffect(() => {
    sparkChatRef.current?.reload();
  }, [
    appBasicConfig?.config.prologue?.prologue_text,
    appBasicConfig?.config.prologue?.suggested_questions,
    appBasicConfig?.name,
    appBasicConfig?.config.modality_type,
  ]);

  useImperativeHandle(ref, () => ({
    onClear,
    resetSession,
    chat: chatRef.current,
  }));

  return (
    <>
      {contextHolder}
      <div key={appCode} style={{ height: '100%' }}>
        <ChatAnywhere
          ref={sparkChatRef}
          onInput={{
            // @ts-ignore
            onSubmit: onInput,
            beforeSubmit: () => Promise.resolve(true),
            maxLength: props.maxTokenContext,
            zoomable: true,
          }}
          uiConfig={{
            welcome: (
              <Welcome
                data={{
                  modalType:
                    appBasicConfig?.config.modality_type || 'textDialog',
                  prologue:
                    appBasicConfig?.config.prologue?.prologue_text || '',
                  suggested_questions: appBasicConfig?.config.prologue
                    ?.suggested_questions?.length
                    ? appBasicConfig?.config.prologue?.suggested_questions
                    : [],
                  name: appBasicConfig?.name || '',
                  icon: '',
                }}
                sparkChatRef={sparkChatRef}
                title={appBasicConfig?.name}
              />
            ),

            mobile: true,
            background: 'transparent',
          }}
          onStop={onStop}
          cardConfig={{
            Steps, // the steps before the content is generated
          }}
          onUpload={[
            // image upload
            {
              multiple: true,
              icon: (
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.App.AssistantAppEdit.components.SparkChat.index.uploadImage',
                    dm: '上传图片进行视觉理解或图片搜索',
                  })}
                >
                  <IconButton
                    icon={
                      <IconFont type="spark-addPicture-line" size="small" />
                    }
                    bordered={false}
                    size="small"
                    disabled={
                      !appBasicConfig?.config.model?.tags?.includes('vision')
                    }
                  ></IconButton>
                </Tooltip>
              ),

              accept: 'image/*',
              customRequest: (options: any) => {
                upload({
                  file: options.file as File,
                  category: 'image',
                  onProgress: (v) => {
                    options?.onProgress?.(v);
                  },
                }).then(async (res) => {
                  options?.onSuccess?.({
                    url: await getPreviewUrl(res.path),
                    downloadUrl: res.path,
                  });
                });
              },
              maxCount: 10,
            },
          ]}
        ></ChatAnywhere>
      </div>
    </>
  );
});
