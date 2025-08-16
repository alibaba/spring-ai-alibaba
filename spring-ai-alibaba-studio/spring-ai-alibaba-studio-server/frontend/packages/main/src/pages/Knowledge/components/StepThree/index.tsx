import SliderInput from '@/components/SliderInput';
import $i18n from '@/i18n';
import { Form, Input } from '@spark-ai/design';
import { Flex } from 'antd';
import classNames from 'classnames';
import React from 'react';
import RadioItem from '../RadioItem';
import styles from './index.module.less';

interface StepThreeProps {
  formRef: React.RefObject<any>;
  changeFormValue: (value: any) => void;
  formValue: any;
}
const chunkOpts = [
  {
    label: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.splitByLength',
      dm: '按长度切分',
    }),
    value: 'length',
    desc: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.suitableForTokenCount',
      dm: '适合对 Token 数量有严格要求的场景，比如使用上下文长度较小的模型时。',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.splitByPage',
      dm: '按分页切分',
    }),
    value: 'page',
    desc: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.suitableForIndependentPages',
      dm: '适合每页传达独立主题的文档，要求不同页面的内容不会混杂在同一文本切片中。',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.splitByTitle',
      dm: '按标题切分',
    }),
    value: 'title',
    desc: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.suitableForIndependentTitles',
      dm: '适合于用标题划分并传达独立主题的文档,要求不同级标题下的内容不会混杂在同一文本切片中。',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.splitByRegex',
      dm: '按正则切分',
    }),
    value: 'regex',
    desc: $i18n.get({
      id: 'main.pages.Knowledge.components.StepThree.index.splitByRegexExpression',
      dm: '依据设置的正则表达式,对知识库中的文本进行切分。',
    }),
  },
];

export const ChunkType = ({ value, onChange, disabled, className }: any) => {
  return (
    <Flex gap={12} wrap>
      {chunkOpts.map((item) => (
        <RadioItem
          className={classNames(styles.mcpInstallTypeItem, className)}
          onSelect={() => {
            onChange(item.value);
          }}
          isActive={value === item.value}
          disabled={disabled}
          {...item}
          key={item.value}
        />
      ))}
    </Flex>
  );
};
export default function StepThree({
  formRef,
  changeFormValue,
  formValue,
}: StepThreeProps) {
  return (
    <div className={styles['step-three']}>
      <Form layout="vertical" ref={formRef}>
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.Knowledge.components.StepThree.index.chunkSplittingMethod',
            dm: 'Chunk切分方式',
          })}
        >
          <ChunkType
            onChange={(value: any) => {
              changeFormValue({
                chunk_type: value,
              });
            }}
            value={formValue.chunk_type}
          />
        </Form.Item>
        {formValue.chunk_type === 'regex' && (
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Knowledge.components.StepThree.index.inputRegularExpression',
              dm: '输入正则表达式',
            })}
            required
          >
            <Input.TextArea
              style={{ height: 58 }}
              value={formValue.regex}
              onChange={(e) => {
                changeFormValue({
                  regex: e.target.value,
                });
              }}
            />
          </Form.Item>
        )}
        {formValue.chunk_type !== 'regex' && (
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Knowledge.components.StepThree.index.segmentEstimatedLength',
              dm: '分段预估长度',
            })}
          >
            <SliderInput
              min={10}
              max={6000}
              step={1}
              style={{ width: 480 }}
              isShowMarker={true}
              value={formValue.chunk_size}
              onChange={(value) => {
                changeFormValue({
                  chunk_size: value,
                });
              }}
            />
          </Form.Item>
        )}
        {(formValue.chunk_type === 'length' ||
          formValue.chunk_type === 'regex') && (
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Knowledge.components.StepThree.index.segmentOverlapLength',
              dm: '分段重叠长度',
            })}
          >
            <SliderInput
              min={1}
              max={1024}
              step={1}
              style={{ width: 480 }}
              isShowMarker={true}
              value={formValue.chunk_overlap}
              onChange={(value) => {
                changeFormValue({
                  chunk_overlap: value,
                });
              }}
            />
          </Form.Item>
        )}
      </Form>
    </div>
  );
}
