import $i18n from '@/i18n';
import { IAppType } from '@/services/appComponent';
import { createApp } from '@/services/appManage';
import uniqueId from '@/utils/uniqueId';
import { Button, getCommonConfig, message, Modal } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Flex } from 'antd';
import classNames from 'classnames';
import { initAppConfig } from '../../utils';
import styles from './index.module.less';

interface ICreateModalProps {
  onCancel: () => void;
  onOk: (val: { type: IAppType; app_id: string }) => void;
}

const options = [
  {
    label: $i18n.get({
      id: 'main.pages.App.components.CreateModal.index.intelligentAgentApp',
      dm: '智能体应用',
    }),
    name: $i18n.get({
      id: 'main.pages.App.components.CreateModal.index.intelligentAgent',
      dm: '智能体',
    }),
    value: 'basic',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.components.Card.index.workflowApp',
      dm: '流程编排应用',
    }),
    name: $i18n.get({
      id: 'main.pages.App.components.CreateModal.index.workflow',
      dm: '流程编排',
    }),
    value: 'workflow',
  },
];

export default function CreateModal(props: ICreateModalProps) {
  const darkMode = getCommonConfig().isDarkMode;

  const [state, setState] = useSetState({
    activeRecord: options[0],
    createLoading: false,
  });
  const createAppByCode = (activeRecord: { name: string; value: IAppType }) => {
    if (state.createLoading) return;

    setState({ createLoading: true });
    createApp({
      name: `${activeRecord.name}_${uniqueId(4)}`,
      type: activeRecord.value as IAppType,
      config: initAppConfig(activeRecord.value as IAppType),
    })
      .then((res) => {
        message.success(
          $i18n.get({
            id: 'main.pages.App.components.CreateModal.index.createSuccess',
            dm: '创建成功',
          }),
        );
        props.onOk({
          type: activeRecord.value as IAppType,
          app_id: res,
        });
        setState({ createLoading: false });
      })
      .catch(() => {
        setState({ createLoading: false });
      });
  };
  return (
    <Modal
      open
      className={styles['app-create-modal']}
      footer={null}
      width={888}
      onCancel={props.onCancel}
      title={$i18n.get({
        id: 'main.pages.App.components.CreateModal.index.createApp',
        dm: '创建应用',
      })}
      styles={{
        body: {
          padding: 40,
        },
      }}
    >
      <Flex gap={40}>
        <div className={classNames(styles['item'])}>
          <img src={`/images/createAgent${darkMode ? 'Dark' : ''}.png`} />
          <div className={styles['header']}>
            <div className={styles['title']}>
              {$i18n.get({
                id: 'main.pages.App.components.CreateModal.index.intelligentAgentApp',
                dm: '智能体应用',
              })}
            </div>
            <Button
              iconType="spark-plus-line"
              type="primary"
              onClick={() =>
                createAppByCode(options[0] as { name: string; value: IAppType })
              }
            >
              {$i18n.get({
                id: 'main.pages.App.components.CreateModal.index.create',
                dm: '创建',
              })}
            </Button>
          </div>
          <div className={styles['desc']}>
            {$i18n.get({
              id: 'main.pages.App.components.CreateModal.index.buildIntelligentAgentApp',
              dm: '构建智能体应用，连接知识、数据与服务，强大的RAG、MCP、插件、记忆及组件能力，适配多种模型，适用于智能助理型、对话型场景。',
            })}
          </div>
        </div>
        <div className={classNames(styles['item'])}>
          <img src={`/images/createFlow${darkMode ? 'Dark' : ''}.png`} />
          <div className={styles['header']}>
            <div className={styles['title']}>
              {$i18n.get({
                id: 'main.pages.App.components.CreateModal.index.workflowApp',
                dm: '工作流编排应用',
              })}
            </div>
            <Button
              iconType="spark-plus-line"
              type="primary"
              onClick={() =>
                createAppByCode(options[1] as { name: string; value: IAppType })
              }
            >
              {$i18n.get({
                id: 'main.pages.App.components.CreateModal.index.create',
                dm: '创建',
              })}
            </Button>
          </div>
          <div className={styles['desc']}>
            {$i18n.get({
              id: 'main.pages.App.components.CreateModal.index.designWorkflow',
              dm: '用户通过画布自定义编排工作流，快速实现业务逻辑设计及效果验证，支持大模型、智能体、组件、API等多种节点，适用于多智能体协同型、流程型场景。',
            })}
          </div>
        </div>
      </Flex>
    </Modal>
  );
}
