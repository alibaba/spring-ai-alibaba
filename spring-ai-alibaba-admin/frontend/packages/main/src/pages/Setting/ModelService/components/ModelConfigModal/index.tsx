import $i18n from '@/i18n';
import {
  ICreateModelParams,
  IModel,
  MODEL_TAGS,
  MODEL_TYPES,
} from '@/types/modelService';
import { Button, Checkbox, Form, Input, Modal, Radio } from '@spark-ai/design';
import { Space } from 'antd';
import React, { useEffect } from 'react';
import styles from './index.module.less';

interface ModelConfigModalProps {
  open: boolean;
  onCancel: () => void;
  onOk: (modelInfo: ICreateModelParams) => void;
  model?: IModel;
  title?: string;
}

const ModelConfigModal: React.FC<ModelConfigModalProps> = ({
  open,
  onCancel,
  onOk,
  model,
  title,
}) => {
  const [form] = Form.useForm();
  const isEdit = !!model?.model_id;
  const modelType = Form.useWatch('type', form);
  const requireTags = modelType === 'llm';

  useEffect(() => {
    if (open) {
      if (model) {
        form.setFieldsValue({
          name: model.name || '',
          type: model.type || 'llm',
          tags: model.tags || [],
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          type: 'llm',
          tags: [],
        });
      }
    }
  }, [open, model, form]);

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      const type = values.type || 'llm';
      let tags: string[] = values.tags || [];
      if (type === 'text_embedding' && tags.length === 0) {
        tags = ['embedding'];
      }
      const _model: ICreateModelParams = {
        ...(model || {}),
        name: values.name,
        model_id: values.name,
        type,
        tags,
      };
      onOk(_model);
    });
  };

  return (
    <Modal
      title={
        title ||
        (isEdit
          ? $i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.editModel',
              dm: '编辑模型',
            })
          : $i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.addModel',
              dm: '新增模型',
            }))
      }
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          {$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.cancel',
            dm: '取消',
          })}
        </Button>,
        <Button key="submit" type="primary" onClick={handleSubmit}>
          {$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.confirm',
            dm: '确认',
          })}
        </Button>,
      ]}
      width={480}
      destroyOnClose={true}
    >
      <div className={styles.container}>
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="name"
            label={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.modelName',
              dm: '模型名称',
            })}
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.enterModelName',
                  dm: '请输入模型名称',
                }),
              },
            ]}
          >
            <Input
              placeholder={$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.enterModelName',
                dm: '请输入模型名称',
              })}
              maxLength={50}
              showCount
            />
          </Form.Item>
          <Form.Item
            name="type"
            label={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.modelType',
              dm: '模型类型',
            })}
            rules={[
              {
                required: true,
                message: $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.selectModelType',
                  dm: '请选择模型类型',
                }),
              },
            ]}
          >
            <Radio.Group>
              <Space wrap>
                {Object.entries(MODEL_TYPES).map(([key, label]) => (
                  <Radio key={key} value={key}>
                    {label}
                  </Radio>
                ))}
              </Space>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            name="tags"
            label={$i18n.get({
              id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.modelAbility',
              dm: '模型能力',
            })}
            rules={[
              {
                required: requireTags,
                message: $i18n.get({
                  id: 'main.pages.Setting.ModelService.components.ModelConfigModal.index.selectAtLeastOneAbility',
                  dm: '请选择至少一个模型能力',
                }),
              },
            ]}
          >
            <Checkbox.Group>
              <div className={styles['capability-options']}>
                {Object.entries(MODEL_TAGS).map(([key, value]) => (
                  <Checkbox key={key} value={key}>
                    {value}
                  </Checkbox>
                ))}
              </div>
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </div>
    </Modal>
  );
};

export default ModelConfigModal;
