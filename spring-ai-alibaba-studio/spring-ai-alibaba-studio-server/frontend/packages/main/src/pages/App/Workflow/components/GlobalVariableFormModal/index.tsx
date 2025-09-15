import $i18n from '@/i18n';
import { updateApp } from '@/services/appManage';
import { IGlobalVariableItem, IWorkFlowAppDetail } from '@/types/appManage';
import uniqueId from '@/utils/uniqueId';
import { Button, IconFont, Input, Modal, message } from '@spark-ai/design';
import {
  IValueType,
  VariableBaseInput,
  VariableTypeSelect,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { omit } from 'lodash-es';
import { useState } from 'react';
import styles from './index.module.less';
export interface IGlobalVariableFormModalProps {
  value: IGlobalVariableItem[];
  onOk: () => void;
  onClose: () => void;
  appDetail: IWorkFlowAppDetail;
}

interface IVariableItem extends IGlobalVariableItem {
  id: string;
}

const initValue = (value: IGlobalVariableItem[]) => {
  return value.map((item) => ({
    ...item,
    id: uniqueId(4),
  }));
};

export default function GlobalVariableFormModal(
  props: IGlobalVariableFormModalProps,
) {
  const [value, setValue] = useState<Array<IVariableItem>>(
    initValue(props.value),
  );
  const [saveLoading, setSaveLoading] = useState(false);

  const changeRowItem = (id: string, payload: Partial<IVariableItem>) => {
    setValue(
      value.map((item) => (item.id === id ? { ...item, ...payload } : item)),
    );
  };

  const removeRowItem = (id: string) => {
    setValue(value.filter((item) => item.id !== id));
  };

  const addRowItem = () => {
    setValue([
      ...value,
      {
        id: uniqueId(4),
        key: '',
        type: 'String',
        desc: '',
        default_value: '',
      },
    ]);
  };

  const validateVariables = (variables: IVariableItem[]): string | null => {
    // check whether there are duplicate keys
    const keys = new Set<string>();
    for (const item of variables) {
      const key = item.key.trim();

      // check whether it is empty
      if (!key) {
        return $i18n.get({
          id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.enterVariableName',
          dm: '请输入变量名',
        });
      }

      // check the format
      if (!/^[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(key)) {
        return $i18n.get({
          id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.variableNameFormat',
          dm: '只能包含字母、数字、下划线和$，且不能以数字开头',
        });
      }

      // check the duplicate
      if (keys.has(key)) {
        return $i18n.get({
          id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.variableNameExists',
          dm: '变量名已存在',
        });
      }
      keys.add(key);
    }
    return null;
  };

  const handleSure = () => {
    const error = validateVariables(value);
    if (error) {
      message.warning(error);
      return;
    }
    setSaveLoading(true);
    const newVals = value.map((item) => omit(item, ['id']));
    updateApp({
      app_id: props.appDetail.app_id,
      name: props.appDetail.name,
      type: props.appDetail.type,
      config: {
        ...props.appDetail.config,
        global_config: {
          ...props.appDetail.config.global_config,
          variable_config: {
            ...props.appDetail.config.global_config.variable_config,
            conversation_params: newVals,
          },
        },
      },
    })
      .then(() => {
        message.success(
          $i18n.get({
            id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.saveSuccess',
            dm: '保存成功',
          }),
        );
        props.onOk();
      })
      .finally(() => {
        setSaveLoading(false);
      });
  };

  return (
    <Modal
      onOk={handleSure}
      width={640}
      onCancel={props.onClose}
      okButtonProps={{
        loading: saveLoading,
        title: saveLoading
          ? $i18n.get({
              id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.saving',
              dm: '保存中...',
            })
          : $i18n.get({
              id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.save',
              dm: '保存',
            }),
      }}
      open
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.conversationVariable',
        dm: '会话变量',
      })}
    >
      <div className={styles.form}>
        <Flex vertical gap={8}>
          <Flex align="center" className={styles.desc} gap={8}>
            <span style={{ width: 120 }}>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.variableName',
                dm: '变量名',
              })}
            </span>
            <span style={{ width: 100 }}>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.variableType',
                dm: '变量类型',
              })}
            </span>
            <span style={{ width: 176 }}>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.variableDescription',
                dm: '变量描述',
              })}
            </span>
            <span>
              {$i18n.get({
                id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.defaultValue',
                dm: '默认值',
              })}
            </span>
          </Flex>
          {value.map((item) => (
            <Flex key={item.id} gap={8} align="center" className={styles.row}>
              <Input
                style={{ width: 120 }}
                value={item.key}
                onChange={(e) =>
                  changeRowItem(item.id, { key: e.target.value })
                }
                placeholder={$i18n.get({
                  id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.enterVariableName',
                  dm: '请输入变量名',
                })}
              />

              <VariableTypeSelect
                type={item.type}
                handleChange={(val) => {
                  changeRowItem(item.id, { type: val as IValueType });
                }}
              />

              <Input
                style={{ width: 176 }}
                value={item.desc}
                onChange={(e) =>
                  changeRowItem(item.id, { desc: e.target.value })
                }
                placeholder={$i18n.get({
                  id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.enterDescription',
                  dm: '请输入描述',
                })}
              />

              <div style={{ width: 140 }}>
                <VariableBaseInput
                  placeholder={$i18n.get({
                    id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.enterDefaultValue',
                    dm: '请输入默认值',
                  })}
                  type={item.type}
                  value={item.default_value}
                  onChange={(val) =>
                    changeRowItem(item.id, { default_value: val.value })
                  }
                />
              </div>
              <IconFont
                type="spark-delete-line"
                isCursorPointer
                size="small"
                onClick={() => removeRowItem(item.id)}
              />
            </Flex>
          ))}
        </Flex>
        <Button
          type="dashed"
          className={styles.add}
          onClick={addRowItem}
          icon={<IconFont type="spark-plus-line" />}
        >
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.GlobalVariableFormModal.index.addVariable',
            dm: '添加变量',
          })}
        </Button>
      </div>
    </Modal>
  );
}
