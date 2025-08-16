import $i18n from '@/i18n';
import { updateApp } from '@/services/appManage';
import { IHistoryConfig, IWorkFlowAppDetail } from '@/types/appManage';
import { IconButton, IconFont, Modal, SliderSelector } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Form, message, Switch, Tooltip } from 'antd';
import { useState } from 'react';
import styles from './index.module.less';
export interface IHistoryConfigModalProps {
  data: IHistoryConfig;
  onOk: () => void;
  onCancel: () => void;
  appDetail: IWorkFlowAppDetail;
}

export default function HistoryConfigModal(props: IHistoryConfigModalProps) {
  const [value, setValue] = useSetState<IHistoryConfig>(props.data);
  const [saveLoading, setSaveLoading] = useState(false);

  const handleOk = () => {
    setSaveLoading(true);
    updateApp({
      app_id: props.appDetail.app_id,
      name: props.appDetail.name,
      type: props.appDetail.type,
      config: {
        ...props.appDetail.config,
        global_config: {
          ...props.appDetail.config.global_config,
          history_config: value,
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
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.HistoryConfigModal.contextRoundSetting',
        dm: '上下文轮次设置',
      })}
      open={true}
      onOk={handleOk}
      okButtonProps={{ loading: saveLoading }}
      onCancel={() => props.onCancel()}
    >
      <Form layout="vertical">
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.App.Workflow.components.HistoryConfigModal.enableContext',
            dm: '是否开启上下文',
          })}
        >
          <Switch
            checked={value.history_switch}
            onChange={(val) => {
              setValue({
                history_switch: val,
              });
            }}
          />
        </Form.Item>
        <Form.Item
          label={$i18n.get({
            id: 'main.pages.App.Workflow.components.HistoryConfigModal.maxMemoryRounds',
            dm: '最大记忆轮次',
          })}
        >
          <SliderSelector
            disabled={!value.history_switch}
            min={1}
            step={1}
            value={value.history_max_round}
            onChange={(val) => {
              setValue({
                history_max_round: val as number,
              });
            }}
            max={50}
            className={styles['slider-selector']}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export function HistoryConfigBtn({
  onSave,
  appDetail,
}: {
  onSave: () => void;
  appDetail: IWorkFlowAppDetail;
}) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Tooltip
        title={$i18n.get({
          id: 'main.pages.App.Workflow.components.HistoryConfigModal.contextRoundSetting',
          dm: '上下文轮次设置',
        })}
      >
        <IconButton
          shape="default"
          icon={<IconFont type="spark-setting-line" />}
          onClick={() => setOpen(true)}
        />
      </Tooltip>
      {open && (
        <HistoryConfigModal
          appDetail={appDetail}
          data={
            appDetail.config.global_config?.history_config || {
              history_switch: true,
              history_max_round: 3,
            }
          }
          onOk={() => {
            onSave();
            setOpen(false);
          }}
          onCancel={() => setOpen(false)}
        />
      )}
    </>
  );
}
