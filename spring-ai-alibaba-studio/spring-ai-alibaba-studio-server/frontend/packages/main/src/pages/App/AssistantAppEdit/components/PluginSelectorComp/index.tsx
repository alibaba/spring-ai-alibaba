import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { ToolSelectorDrawer } from '@/pages/App/components/PluginSelector';
import { PluginTool } from '@/types/plugin';
import { Button, HelpIcon, IconFont } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Divider, Flex } from 'antd';
import cls from 'classnames';
import { useContext, useEffect } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import SelectedConfigItem from '../SelectedConfigItem';
import styles from './index.module.less';

export const TOOL_MAX_LIMIT = defaultSettings.agentToolMaxLimit;

export function SelectedToolItem({
  tool,
  handleRemoveTool,
}: {
  tool: PluginTool;
  handleRemoveTool: (tool: PluginTool) => void;
}) {
  return (
    <SelectedConfigItem
      iconType="spark-plugin-line"
      name={tool.name}
      rightArea={
        <IconFont
          type="spark-delete-line"
          isCursorPointer
          onClick={() => {
            handleRemoveTool(tool);
          }}
        ></IconFont>
      }
    ></SelectedConfigItem>
  );
}

export default function PluginSelectorComp() {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const { tools: selectedTools = [] } = appState.appBasicConfig?.config || {};
  const [state, setState] = useSetState({
    expand: false,
    selectVisible: false,
  });

  const handleSelectTools = (tools: PluginTool[]) => {
    onAppConfigChange({ tools });
  };

  useEffect(() => {
    if (selectedTools.length) {
      setState({ expand: true });
    }
  }, [selectedTools]);

  const handleRemoveTool = (tool: PluginTool) => {
    handleSelectTools(
      selectedTools.filter((vItem) => vItem.tool_id !== tool.tool_id),
    );
  };

  return (
    <Flex vertical gap={6} className="mb-[20px]">
      <div className={styles.titleWrap}>
        <Flex
          gap={8}
          className="text-[13px] font-medium leading-[20px]"
          style={{ color: 'var(--ag-ant-color-text)' }}
          align="center"
        >
          <Flex align="center">
            <span>
              {$i18n.get({
                id: 'main.components.PluginSelectorComp.index.plugin',
                dm: '插件',
              })}
            </span>
            <HelpIcon
              content={$i18n.get({
                id: 'main.components.PluginSelectorComp.index.callOpenApi',
                dm: '智能体可以通过插件主动调用OpenAPI，例如信息查询、数据存储等。',
              })}
            ></HelpIcon>
          </Flex>
          <span
            className="text-[12px] leading-[20px]"
            style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
          >
            {selectedTools.length}/{TOOL_MAX_LIMIT}
          </span>
        </Flex>
        <span className={styles.right}>
          <Button
            style={{ padding: 0 }}
            onClick={() => setState({ selectVisible: true })}
            iconType="spark-plus-line"
            type="text"
            size="small"
          >
            {$i18n.get({
              id: 'main.components.PluginSelectorComp.index.plugin',
              dm: '插件',
            })}
          </Button>
          <Divider type="vertical" className="ml-[16px] mr-[16px]"></Divider>
          <IconFont
            onClick={() => setState({ expand: !state.expand })}
            className={cls(styles.expandBtn, !state.expand && styles.hidden)}
            type="spark-up-line"
          />
        </span>
      </div>
      {state.expand && (
        <Flex vertical gap={8}>
          {selectedTools.map((item) => (
            <SelectedToolItem
              handleRemoveTool={() => handleRemoveTool(item)}
              tool={item}
              key={item.tool_id}
            />
          ))}
        </Flex>
      )}

      {state.selectVisible && (
        <ToolSelectorDrawer
          maxLen={TOOL_MAX_LIMIT}
          value={selectedTools}
          onOk={handleSelectTools}
          onClose={() => setState({ selectVisible: false })}
        />
      )}
    </Flex>
  );
}
