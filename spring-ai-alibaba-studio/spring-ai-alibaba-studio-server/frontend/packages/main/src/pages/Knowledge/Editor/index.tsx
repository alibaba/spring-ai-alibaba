import InnerLayout from '@/components/InnerLayout';
import SliderInput from '@/components/SliderInput';
import $i18n from '@/i18n';
import ModelSelector from '@/pages/Knowledge/components/ModelSelector';
import { getKnowledgeDetail, updateKnowledge } from '@/services/knowledge';
import { IKnowledgeDetail } from '@/types/knowledge';
import {
  Button,
  Form,
  IconFont,
  Input,
  message,
  Tooltip,
} from '@spark-ai/design';
import { useRequest, useSetState } from 'ahooks';
import { Modal as AntModal } from 'antd';
import { useRef } from 'react';
import { useParams } from 'react-router-dom';
import { history } from 'umi';
import styles from './index.module.less';

export default function Editor() {
  const { kb_id } = useParams<{ kb_id: string }>();
  const formRef = useRef<any>(null);
  const [state, setState] = useSetState({
    name: '',
    description: '',
    embedding_value: '',
    embedding_model: '',
    embedding_provider: '',
    rerank_value: '',
    rerank_model: '',
    rerank_provider: '',
    similarity_threshold: 0.2,
    top_k: 3,
  });
  useRequest(() => getKnowledgeDetail(kb_id || ''), {
    onSuccess: (data: IKnowledgeDetail) => {
      const { index_config, search_config } = data;
      setState({
        name: data.name,
        description: data.description,
        embedding_value: index_config.embedding_model
          ? `${index_config.embedding_provider}@@@${index_config.embedding_model}`
          : '',
        embedding_model: index_config?.embedding_model,
        embedding_provider: index_config?.embedding_provider,
        rerank_value: search_config.rerank_model
          ? `${search_config.rerank_provider}@@@${search_config.rerank_model}`
          : '',
        rerank_model: search_config?.rerank_model,
        rerank_provider: search_config?.rerank_provider,
        similarity_threshold: search_config.similarity_threshold,
        top_k: search_config.top_k,
      });
    },
  });

  const changeFormValue = (payload: any) => {
    setState((prev) => ({
      ...prev,
      ...payload,
    }));
  };
  const handleSave = () => {
    validatedFormValues()
      .then(() => {
        const {
          top_k,
          similarity_threshold,
          rerank_provider,
          rerank_model,
          embedding_provider,
          embedding_model,
          ...rest
        } = state;
        const params = {
          kb_id: kb_id?.toString() || '',
          search_config: {
            top_k,
            similarity_threshold,
            rerank_provider,
            rerank_model,
          },
          index_config: {
            embedding_provider,
            embedding_model,
          },
          ...rest,
        };
        updateKnowledge(params).then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.Knowledge.Editor.index.saveSuccess',
              dm: '保存成功',
            }),
          );
          history.push('/knowledge');
        });
      })
      .catch((err) => {
        message.error(err.message);
      });
  };
  const validatedFormValues = () => {
    return new Promise((resolve, reject) => {
      if (!state.name?.trim()) {
        reject(
          $i18n.get({
            id: 'main.pages.Knowledge.Create.index.pleaseEnterKnowledgeBaseName',
            dm: '请先填写知识库名称',
          }),
        );
        return;
      }
      if (!state.embedding_value?.trim()) {
        reject(
          $i18n.get({
            id: 'main.pages.Knowledge.Create.index.pleaseSelectEmbeddingModel',
            dm: '请先选择Embedding模型',
          }),
        );
        return;
      }

      if (!state.rerank_value?.trim()) {
        reject(
          $i18n.get({
            id: 'main.pages.Knowledge.Create.index.pleaseSelectRerankModel',
            dm: '请先选择Rerank模型',
          }),
        );
        return;
      }
      resolve(state);
    });
  };
  const handleCancel = () => {
    AntModal.confirm({
      title: (
        <span className={styles['confirm-title']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Editor.index.confirmDiscardEditing',
            dm: '确认放弃编辑知识库吗',
          })}
        </span>
      ),

      icon: (
        <IconFont
          type="spark-warningCircle-line"
          className={styles['warning-icon']}
        />
      ),

      content: (
        <span className={styles['confirm-content']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Editor.index.discardChangesDataNotSaved',
            dm: '放弃编辑后您刚刚填写的数据将不会被保存，请谨慎操作',
          })}
        </span>
      ),

      okText: $i18n.get({
        id: 'main.pages.Knowledge.Editor.index.confirmDiscard',
        dm: '确认放弃',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.Knowledge.Editor.index.continueEditing',
        dm: '继续编辑',
      }),
      onOk: () => {
        history.push('/knowledge');
      },
    });
  };
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Editor.index.knowledgeBase',
            dm: '知识库',
          }),
          path: '/knowledge',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Editor.index.editKnowledgeBase',
            dm: '编辑知识库',
          }),
        },
      ]}
      bottom={
        <div className={styles['footer']}>
          <div className={styles['footer-btn']}>
            <Button
              type="primary"
              onClick={() => {
                validatedFormValues()
                  .then(() => {
                    handleSave();
                  })
                  .catch((errInfo) => message.warning(errInfo));
              }}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Editor.index.save',
                dm: '保存',
              })}
            </Button>
            <Button onClick={handleCancel}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Editor.index.cancel',
                dm: '取消',
              })}
            </Button>
          </div>
        </div>
      }
    >
      <div className={styles['container']}>
        <Form layout="vertical" ref={formRef}>
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Knowledge.Editor.index.knowledgeBaseName',
              dm: '知识库名称',
            })}
            required
          >
            <Input
              placeholder={$i18n.get({
                id: 'main.pages.Knowledge.Editor.index.enterKnowledgeBaseName',
                dm: '请输入知识库名称',
              })}
              value={state.name}
              onChange={(e) => {
                changeFormValue({ name: e.target.value });
              }}
            />
          </Form.Item>
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Knowledge.Editor.index.knowledgeBaseDescription',
              dm: '知识库描述',
            })}
          >
            <Input.TextArea
              placeholder={$i18n.get({
                id: 'main.pages.Knowledge.Editor.index.enterKnowledgeBaseDescription',
                dm: '请输入知识库描述',
              })}
              value={state.description}
              onChange={(e) => {
                changeFormValue({ description: e.target.value });
              }}
            />
          </Form.Item>
          <Form.Item
            label={
              <div className={styles['form-item-label']}>
                <span>
                  {$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.embeddingModel',
                    dm: 'Embedding模型',
                  })}
                </span>
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.modelConvertTextToVector',
                    dm: '用于将文本转换为向量表示的模型，能把文本信息映射到低维稠密向量空间，便于计算机理解文本语义，支持后续的相似性计算等操作',
                  })}
                >
                  <IconFont
                    type="spark-info-line"
                    className={styles['info-icon']}
                  />
                </Tooltip>
              </div>
            }
            required
          >
            <ModelSelector
              value={state.embedding_value}
              modelType="text_embedding"
              onChange={(val: string) => {
                changeFormValue({
                  embedding_value: val,
                  embedding_model: val.split('@@@')[1],
                  embedding_provider: val.split('@@@')[0],
                });
              }}
            />
          </Form.Item>
          <Form.Item
            label={
              <div className={styles['form-item-label']}>
                <span>
                  {$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.rerankModel',
                    dm: 'Rerank模型',
                  })}
                </span>
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.rerankModelReorderResults',
                    dm: 'Rerank模型在检索出相关结果后，对结果进行重新排序，通过更精准的算法调整结果顺序，使相关性更高的内容排在前列，提升检索结果质量。',
                  })}
                >
                  <IconFont
                    type="spark-info-line"
                    className={styles['info-icon']}
                  />
                </Tooltip>
              </div>
            }
            required
          >
            <ModelSelector
              value={state.rerank_value}
              modelType="rerank"
              onChange={(val: string) => {
                changeFormValue({
                  rerank_value: val,
                  rerank_model: val.split('@@@')[1],
                  rerank_provider: val.split('@@@')[0],
                });
              }}
            />
          </Form.Item>
          <Form.Item
            label={
              <div className={styles['form-item-label']}>
                <span>
                  {$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.similarityThreshold',
                    dm: '相似度阈值',
                  })}
                </span>
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.thresholdMeasureSimilarity',
                    dm: '用于衡量文本或数据间相似程度的临界值,当计算出的文本相似度达到或超过该值时，会返回该文本 。',
                  })}
                >
                  <IconFont
                    type="spark-info-line"
                    className={styles['info-icon']}
                  />
                </Tooltip>
              </div>
            }
          >
            <SliderInput
              min={0.01}
              max={0.99}
              step={0.01}
              style={{ width: 480 }}
              value={state.similarity_threshold}
              onChange={(val) => {
                changeFormValue({ similarity_threshold: val });
              }}
            />
          </Form.Item>
          <Form.Item
            label={
              <div className={styles['form-item-label']}>
                <span>Topk</span>
                <Tooltip
                  title={$i18n.get({
                    id: 'main.pages.Knowledge.Editor.index.topKReturnObjects',
                    dm: 'top-k代表重排后返回的符合相似度要求的对象个数',
                  })}
                >
                  <IconFont
                    type="spark-info-line"
                    className={styles['info-icon']}
                  />
                </Tooltip>
              </div>
            }
          >
            <SliderInput
              min={1}
              max={10}
              step={1}
              style={{ width: 480 }}
              value={state.top_k}
              onChange={(val) => {
                changeFormValue({ top_k: val });
              }}
            />
          </Form.Item>
        </Form>
      </div>
    </InnerLayout>
  );
}
