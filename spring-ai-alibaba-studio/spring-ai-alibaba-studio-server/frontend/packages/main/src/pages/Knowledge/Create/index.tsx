import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { createDocuments, createKnowledge } from '@/services/knowledge';
import { AlertDialog, Button, message } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Steps } from 'antd';
import { useRef, useState } from 'react';
import { history } from 'umi';
import StepOne from '../components/StepOne';
import StepThree from '../components/StepThree';
import StepTwo from '../components/StepTwo';
import styles from './index.module.less';
interface FormValue {
  name: string;
  description: string;
  enable_rewrite: boolean;
  embedding_value: string;
  embedding_model: string;
  embedding_provider: string;
  rerank_value: string;
  rerank_model: string;
  rerank_provider: string;
  similarity_threshold: number;
  top_k: number;
  sourceType: string;
  upload: string;
  tags: string[];
  meta: boolean;
  chunk_type: string;
  regex: string;
  chunk_size: number;
  chunk_overlap: number;
  fileList: any[];
}

interface State {
  isChanged: boolean;
  quitConfirm: boolean;
  step: number;
  formValue: FormValue;
  countdown: number;
}
export default function Ceeate() {
  const firstFormRef = useRef<any>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [state, setState] = useSetState<State>({
    isChanged: false,
    quitConfirm: false,
    step: 0,
    formValue: {
      name: '',
      description: '',
      enable_rewrite: false,
      embedding_value: '',
      embedding_model: '',
      embedding_provider: '',
      rerank_value: '',
      rerank_model: '',
      rerank_provider: '',
      similarity_threshold: 0.2,
      top_k: 3,
      sourceType: 'file',
      upload: '',
      tags: [],
      meta: false,
      chunk_type: 'length',
      regex: '',
      chunk_size: 600,
      chunk_overlap: 100,
      fileList: [],
    },
    countdown: 3,
  });
  const [fileList, setFileList] = useState<any[]>([]);
  const [uploadingCount, setUploadingCount] = useState(0);
  const handleCancel = () => {
    AlertDialog.warning({
      title: (
        <span className={styles['confirm-title']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Create.index.confirmDiscardCreation',
            dm: '确认放弃创建知识库吗',
          })}
        </span>
      ),

      content: (
        <span className={styles['confirm-content']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Create.index.discardCreationDataWillNotBeSaved',
            dm: '放弃创建后您刚刚填写的数据将不会被保存，请谨慎操作',
          })}
        </span>
      ),

      okText: $i18n.get({
        id: 'main.pages.Knowledge.Create.index.confirmDiscard',
        dm: '确认放弃',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.Knowledge.Create.index.continueEditing',
        dm: '继续编辑',
      }),
      onOk: () => {
        history.push('/knowledge');
      },
      onCancel: () => {
        setState({ quitConfirm: false });
      },
    });
  };

  const changeFormValue = (payload: any) => {
    setState((prev) => ({
      ...prev,
      isChanged: true,
      formValue: {
        ...prev.formValue,
        ...payload,
      },
    }));
  };

  const validatedFormValues = () => {
    return new Promise((resolve, reject) => {
      const { formValue } = state;
      if (state.step === 0) {
        if (!formValue.name?.trim()) {
          reject(
            $i18n.get({
              id: 'main.pages.Knowledge.Create.index.pleaseEnterKnowledgeBaseName',
              dm: '请先填写知识库名称',
            }),
          );
          return;
        }
        if (!formValue.embedding_model) {
          reject(
            $i18n.get({
              id: 'main.pages.Knowledge.Create.index.pleaseSelectEmbeddingModel',
              dm: '请先选择Embedding模型',
            }),
          );
          return;
        }

        if (!formValue.rerank_model) {
          reject(
            $i18n.get({
              id: 'main.pages.Knowledge.Create.index.pleaseSelectRerankModel',
              dm: '请先选择Rerank模型',
            }),
          );
          return;
        }
      }
      resolve(formValue);
    });
  };

  const handleSubmit = (type: string) => {
    validatedFormValues().then(() => {
      const formValue = state.formValue;
      const params = {
        name: formValue.name,
        description: formValue.description,
        process_config: {
          chunk_type: formValue.chunk_type,
          chunk_size: formValue.chunk_size,
          chunk_overlap: formValue.chunk_overlap,
        },
        index_config: {
          embedding_provider: formValue.embedding_provider,
          embedding_model: formValue.embedding_model,
        },
        search_config: {
          top_k: formValue.top_k,
          similarity_threshold: formValue.similarity_threshold,
          rerank_provider: formValue.rerank_provider,
          rerank_model: formValue.rerank_model,
        },
      };
      createKnowledge(params).then((id) => {
        setIsSuccess(true);
        if (type === 'done' && fileList.length) {
          handleCreateDocument(id);
        }
        let countdown = (state.countdown = 3);
        const interval = setInterval(() => {
          countdown -= 1;
          setState({ countdown });
          if (countdown === 0) {
            clearInterval(interval);
            history.push('/knowledge');
          }
        }, 1000);
      });
    });
  };

  const handleCreateDocument = (id: any) => {
    const { formValue } = state;
    const params = {
      kb_id: id,
      type: formValue.sourceType as 'file' | 'url',
      files: fileList,
      process_config: {
        chunk_type: formValue.chunk_type,
        chunk_size: formValue.chunk_size,
        chunk_overlap: formValue.chunk_overlap,
      },
    };
    createDocuments(params);
  };
  const bottomButton = () => {
    return (
      <div className={styles['footer']}>
        {state.step === 0 && (
          <div className={styles['btn-group']}>
            <Button
              type="primary"
              onClick={() => {
                validatedFormValues()
                  .then(() => {
                    setState({ step: 1 });
                  })
                  .catch((errInfo) => message.warning(errInfo));
              }}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.nextStep',
                dm: '下一步',
              })}
            </Button>
            <Button onClick={handleCancel}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.cancel',
                dm: '取消',
              })}
            </Button>
            <div className={styles['divider']} />
            <Button type="default" onClick={() => handleSubmit('next')}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.directCreate',
                dm: '直接创建',
              })}
            </Button>
          </div>
        )}
        {state.step === 1 && (
          <div className={styles['btn-group']}>
            <Button
              type="default"
              onClick={() => {
                setState({ step: 0 });
              }}
              disabled={uploadingCount > 0}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.previousStep',
                dm: '上一步',
              })}
            </Button>
            <Button
              type="primary"
              onClick={() => {
                validatedFormValues()
                  .then(() => {
                    setState({ step: 2 });
                  })
                  .catch((errInfo) => message.warning(errInfo));
              }}
              disabled={uploadingCount > 0}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.nextStep',
                dm: '下一步',
              })}
            </Button>
            <Button onClick={handleCancel}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.cancel',
                dm: '取消',
              })}
            </Button>
          </div>
        )}
        {state.step === 2 && (
          <div className={styles['btn-group']}>
            <Button
              type="default"
              onClick={() => {
                setState({ step: 1 });
              }}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.previousStep',
                dm: '上一步',
              })}
            </Button>
            <Button type="primary" onClick={() => handleSubmit('done')}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.complete',
                dm: '完成',
              })}
            </Button>
            <Button onClick={handleCancel}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Create.index.cancel',
                dm: '取消',
              })}
            </Button>
          </div>
        )}
      </div>
    );
  };
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Create.index.knowledgeBase',
            dm: '知识库',
          }),
          path: '/knowledge',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Create.index.createKnowledgeBase',
            dm: '创建知识库',
          }),
        },
      ]}
      bottom={bottomButton()}
    >
      <div className={styles['container']}>
        <div className={styles['steps']}>
          <Steps
            current={state.step}
            labelPlacement="vertical"
            size="small"
            items={[
              {
                title: (
                  <span className={styles['steps-title']}>
                    {$i18n.get({
                      id: 'main.pages.Knowledge.Create.index.knowledgeBaseInformation',
                      dm: '知识库信息',
                    })}
                  </span>
                ),
                status: state.step === 0 ? 'process' : 'finish',
              },
              {
                title: (
                  <span className={styles['steps-title']}>
                    {$i18n.get({
                      id: 'main.pages.Knowledge.Create.index.selectData',
                      dm: '选择数据',
                    })}
                  </span>
                ),
                status: state.step === 2 ? 'finish' : 'process',
              },
              {
                title: (
                  <span className={styles['steps-title']}>
                    {$i18n.get({
                      id: 'main.pages.Knowledge.Create.index.dataProcessing',
                      dm: '数据处理',
                    })}
                  </span>
                ),
                status: 'process',
              },
            ]}
          />
        </div>
        {state.step === 0 && (
          <div className={styles['content']}>
            <StepOne
              formRef={firstFormRef}
              changeFormValue={changeFormValue}
              formValue={state.formValue}
            />
          </div>
        )}
        {state.step === 1 && (
          <div className={styles['content']}>
            <StepTwo
              formRef={firstFormRef}
              changeFormValue={changeFormValue}
              formValue={state.formValue}
              setFileList={setFileList}
              fileList={fileList}
              setUploadingCount={setUploadingCount}
              uploadingCount={uploadingCount}
            />
          </div>
        )}
        {state.step === 2 && (
          <div className={styles['content']}>
            <StepThree
              formRef={firstFormRef}
              changeFormValue={changeFormValue}
              formValue={state.formValue}
            />
          </div>
        )}
      </div>
      {isSuccess && (
        <AlertDialog
          type="success"
          title={$i18n.get({
            id: 'main.pages.Knowledge.Create.index.knowledgeBaseCreatedSuccessfully',
            dm: '知识库创建成功',
          })}
          open={true}
          footer={
            <Button
              type="primary"
              onClick={() => {
                setIsSuccess(false);
                history.push('/knowledge');
              }}
            >
              {$i18n.get(
                {
                  id: 'main.pages.Knowledge.Create.index.returnToKnowledgeBaseManagementVar1S',
                  dm: '返回知识库管理 （{var1}S）',
                },
                { var1: state.countdown },
              )}
            </Button>
          }
        >
          {$i18n.get({
            id: 'main.pages.Knowledge.Create.index.congratulationsYouHaveCompletedKnowledgeBaseCreationYouCanGoToConfigureApplication',
            dm: '恭喜，您已完成知识库创建，可以去配置应用了',
          })}
        </AlertDialog>
      )}
    </InnerLayout>
  );
}
