import SliderInput from '@/components/SliderInput';
import $i18n from '@/i18n';
import { Button, Form, IconFont, Input, Tooltip } from '@spark-ai/design';
import classNames from 'classnames';
import React, { useEffect } from 'react';
import styles from './index.module.less';
interface FormProps {
  /**
   * Custom style
   */
  className?: string;
  /**
   * Submit callback
   */
  onSubmit?: (values: any) => void;
  /**
   * Similarity threshold
   */
  similarity_threshold?: number;
}

const initialValues = {
  similarity_threshold: 0.2,
  input: '',
};
const TestForm: React.FC<FormProps> = ({
  onSubmit,
  className,
  similarity_threshold,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({ similarity_threshold });
  }, [similarity_threshold]);

  return (
    <div className={classNames(styles['form-container'], className)}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        initialValues={initialValues}
      >
        <div className={styles['form-title']}>
          {$i18n.get({
            id: 'main.pages.Knowledge.Test.components.Form.index.databaseConfigDebug',
            dm: '数据库配置调试',
          })}
        </div>
        <Form.Item
          label={
            <div className={styles['form-item-label']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Knowledge.Test.components.Form.index.similarityThreshold',
                  dm: '相似度阈值',
                })}
              </span>
              <Tooltip
                title={$i18n.get({
                  id: 'main.pages.Knowledge.Test.components.Form.index.thresholdMeasureSimilarity',
                  dm: '用于衡量文本或数据间相似程度的临界值,当计算出的文本相似度达到或超过该值时，会返回该文本 。',
                })}
                placement="rightBottom"
              >
                <IconFont
                  type="spark-info-line"
                  className={styles['info-icon']}
                />
              </Tooltip>
            </div>
          }
          name="similarity_threshold"
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Knowledge.Test.components.Form.index.enterSimilarityThreshold',
                dm: '请输入相似度阈值',
              }),
            },
          ]}
        >
          <SliderInput min={0.01} max={0.99} step={0.01} />
        </Form.Item>

        <Form.Item
          label={$i18n.get({
            id: 'main.pages.Knowledge.Test.components.Form.index.input',
            dm: '输入',
          })}
          name="query"
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Knowledge.Test.components.Form.index.enter',
                dm: '请输入',
              }),
            },
          ]}
        >
          <Input.TextArea
            placeholder={$i18n.get({
              id: 'main.pages.Knowledge.Test.components.Form.index.enter',
              dm: '请输入',
            })}
            className={styles['text-area']}
            autoSize={{ minRows: 4, maxRows: 4 }}
          />
        </Form.Item>

        <Form.Item className={styles['form-item-submit']}>
          <Button
            type="default"
            icon={<IconFont type="spark-testing-line" />}
            className={styles['submit-button']}
            htmlType="submit"
          >
            {$i18n.get({
              id: 'main.pages.Knowledge.Test.components.Form.index.test',
              dm: '测试',
            })}
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

export default TestForm;
