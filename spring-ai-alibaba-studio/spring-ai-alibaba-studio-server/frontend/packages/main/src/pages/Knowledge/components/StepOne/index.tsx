import SliderInput from '@/components/SliderInput';
import $i18n from '@/i18n';
import { Form, IconFont, Input, Tooltip } from '@spark-ai/design';
import React, { useRef } from 'react';
import ModelSelector from '../ModelSelector';
import styles from './index.module.less';

interface FormValue {
  name: string;
  description?: string;
  embedding_value?: string;
  embedding_model?: string;
  embedding_provider?: string;
  rerank_value?: string;
  rerank_model?: string;
  rerank_provider?: string;
  similarity_threshold?: number;
  top_k?: number;
  enable_rewrite?: boolean;
}

interface StepOneProps {
  formRef: React.RefObject<any>;
  changeFormValue: (value: Partial<FormValue>) => void;
  formValue: FormValue;
}

export default function StepOne({
  formRef,
  changeFormValue,
  formValue,
}: StepOneProps) {
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  return (
    <div className={styles['step-one']}>
      <Form layout="vertical" ref={formRef}>
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.Knowledge.components.StepOne.index.knowledgeBaseName',
            dm: '知识库名称',
          })}
          required
        >
          <Input
            className={styles.input}
            value={formValue.name}
            onChange={(e) => {
              const newName = e.target.value;
              changeFormValue({ name: newName });
            }}
            showCount
            maxLength={15}
            placeholder={$i18n.get({
              id: 'main.pages.Knowledge.components.StepOne.index.enterKnowledgeBaseName',
              dm: '请输入知识库名称',
            })}
            onBlur={() => {
              if (timerRef.current) {
                clearTimeout(timerRef.current);
                timerRef.current = null;
              }
            }}
          />
        </Form.Item>
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.Knowledge.components.StepOne.index.knowledgeBaseDescription',
            dm: '知识库描述',
          })}
          name="description"
        >
          <Input.TextArea
            placeholder={$i18n.get({
              id: 'main.pages.Knowledge.components.StepOne.index.enterKnowledgeBaseDescription',
              dm: '请输入知识库描述',
            })}
            value={formValue.description}
            style={{ height: 100 }}
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
                  id: 'main.pages.Knowledge.components.StepOne.index.embeddingModel',
                  dm: 'Embedding模型',
                })}
              </span>

              <Tooltip
                title={$i18n.get({
                  id: 'main.pages.Knowledge.components.StepOne.index.modelConvertTextToVector',
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
            value={formValue.embedding_value}
            modelType="text_embedding"
            onChange={(val: string) => {
              changeFormValue({
                embedding_value: val,
                embedding_model: val?.split('@@@')[1],
                embedding_provider: val?.split('@@@')[0],
              });
            }}
          />
        </Form.Item>
        <Form.Item
          label={
            <div className={styles['form-item-label']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Knowledge.components.StepOne.index.rerankModel',
                  dm: 'Rerank模型',
                })}
              </span>
              <Tooltip
                title={$i18n.get({
                  id: 'main.pages.Knowledge.components.StepOne.index.rerankModelReorderResults',
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
            value={formValue.rerank_value}
            modelType="rerank"
            onChange={(val: string) => {
              changeFormValue({
                rerank_value: val,
                rerank_model: val?.split('@@@')[1],
                rerank_provider: val?.split('@@@')[0],
              });
            }}
          />
        </Form.Item>
        <Form.Item
          label={
            <div className={styles['form-item-label']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Knowledge.components.StepOne.index.similarityThreshold',
                  dm: '相似度阈值',
                })}
              </span>
              <Tooltip
                title={$i18n.get({
                  id: 'main.pages.Knowledge.components.StepOne.index.thresholdMeasureSimilarity',
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
            value={formValue.similarity_threshold}
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
                  id: 'main.pages.Knowledge.components.StepOne.index.topKReturnObjects',
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
            value={formValue.top_k}
            onChange={(val) => {
              changeFormValue({ top_k: val });
            }}
          />
        </Form.Item>
      </Form>
    </div>
  );
}
