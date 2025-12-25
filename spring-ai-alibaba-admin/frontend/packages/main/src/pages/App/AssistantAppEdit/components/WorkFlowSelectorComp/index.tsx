import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { IAppType } from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { Button, HelpIcon, IconFont } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Divider, Flex } from 'antd';
import cls from 'classnames';
import { useContext, useEffect } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import ComponentSelectorDrawer from '../ComponentSelectorDrawer';
import SelectedConfigItem from '../SelectedConfigItem';
import styles from './index.module.less';

const MAX_LIMIT = defaultSettings.agentWorkflowComponentMaxLimit;

export default function WorkFlowSelectorComp() {
  const { appState, onAppConfigChange, appCode } =
    useContext(AssistantAppContext);
  const { workflow_components = [] } = appState.appBasicConfig?.config || {};
  const [state, setState] = useSetState({
    expand: false,
    selectVisible: false,
  });

  const onSelect = (val: IAppComponentListItem[]) => {
    onAppConfigChange({ workflow_components: val });
    setState({ selectVisible: false });
  };

  useEffect(() => {
    if (workflow_components.length) {
      setState({ expand: true });
    }
  }, [workflow_components]);

  const onRemove = (val: string) => {
    onSelect(workflow_components.filter((vItem) => vItem.code !== val));
  };

  return (
    <Flex vertical gap={6} className="mb-[20px]">
      <Flex justify="space-between">
        <Flex
          gap={8}
          className="text-[13px] font-medium leading-[20px]"
          style={{ color: 'var(--ag-ant-color-text-base)' }}
          align="center"
        >
          <Flex align="center">
            <span>
              {$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.WorkFlowSelectorComp.index.workflow',
                dm: '工作流',
              })}
            </span>

            <HelpIcon
              content={$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.WorkFlowSelectorComp.index.workflowDescription',
                dm: '编排好的工作流应用可发布为工作流组件，从而实现复杂、稳定的业务流程。',
              })}
            ></HelpIcon>
          </Flex>
          <span
            className="text-[12px] leading-[24px]"
            style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
          >
            {workflow_components.length}/{MAX_LIMIT}
          </span>
        </Flex>
        <span>
          <Button
            style={{ padding: 0 }}
            onClick={() => setState({ selectVisible: true })}
            iconType="spark-plus-line"
            type="text"
            size="small"
          >
            {$i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.WorkFlowSelectorComp.index.workflow',
              dm: '工作流',
            })}
          </Button>
          <Divider type="vertical" className="ml-[16px] mr-[16px]"></Divider>
          <IconFont
            onClick={() => setState({ expand: !state.expand })}
            className={cls(styles.expandBtn, !state.expand && styles.hidden)}
            type="spark-up-line"
            isCursorPointer
          />
        </span>
      </Flex>
      {state.expand && (
        <Flex vertical gap={8}>
          {workflow_components.map(
            (item) =>
              item && (
                <SelectedConfigItem
                  key={item.code}
                  iconType="spark-processJudgment-line"
                  name={item.name!}
                  rightArea={
                    <IconFont
                      type="spark-delete-line"
                      isCursorPointer
                      onClick={() => {
                        onRemove(item.code!);
                      }}
                    ></IconFont>
                  }
                />
              ),
          )}
        </Flex>
      )}
      {state.selectVisible && (
        <ComponentSelectorDrawer
          maxLength={MAX_LIMIT}
          selected={workflow_components}
          onClose={() => {
            setState({ selectVisible: false });
          }}
          type={IAppType.WORKFLOW}
          onSelect={onSelect}
          appCode={appCode || ''}
        />
      )}
    </Flex>
  );
}
