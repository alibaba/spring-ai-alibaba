import $i18n from '@/i18n';
import { listPlugin } from '@/services/plugin';
import { ListPluginParams, Plugin, PluginTool } from '@/types/plugin';
import { Button, Drawer, Input, Modal } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Empty, Flex, message, Pagination, Spin } from 'antd';
import classNames from 'classnames';
import { useMemo, useState } from 'react';
import { TOOL_MAX_LIMIT } from '../../AssistantAppEdit/components/PluginSelectorComp';
import styles from './index.module.less';
import PluginListItem from './PluginListItem';

interface IProps {
  className: string;
  value?: Array<PluginTool>;
  onChange?: (val: Array<PluginTool>) => void;
  maxLength?: number;
}

export default function ToolSelector(props: IProps) {
  const { value = [], maxLength = TOOL_MAX_LIMIT } = props;
  const [state, setState] = useSetState({
    current: 1,
    size: 10,
    name: '',
    loading: true,
    list: [] as Array<Plugin>,
    total: 0,
  });

  const fetchList = (extraParams: ListPluginParams = {}) => {
    const params = {
      current: state.current,
      size: state.size,
      ...extraParams,
    };
    setState({ loading: true });
    listPlugin(params).then((res) => {
      setState({
        list: res.data.records,
        total: res.data.total,
        loading: false,
      });
    });
  };

  useMount(() => {
    fetchList();
  });

  const handleAddTools = (tools: PluginTool[]) => {
    if (value.length >= maxLength) {
      message.warning(
        $i18n.get({
          id: 'main.pages.App.components.PluginSelector.index.reachMaxLimit',
          dm: '已达到最大数量限制',
        }),
      );
      return;
    }
    const newTools = [...value, ...tools];
    props.onChange?.(newTools);
  };

  const handleRemove = (toolItem: PluginTool) => {
    props.onChange?.(value.filter((item) => item.tool_id !== toolItem.tool_id));
  };

  return (
    <Flex className={classNames('p-[8px]', props.className)} vertical>
      {
        <Flex justify="space-between" align="center">
          <Input.Search
            style={{ width: 280 }}
            width={280}
            value={state.name}
            onChange={(e) => setState({ name: e.target.value })}
            placeholder={$i18n.get({
              id: 'main.pages.App.components.PluginSelector.index.searchPluginName',
              dm: '搜索插件名称',
            })}
            onSearch={() => {
              setState({ current: 1 });
              fetchList({ current: 1, name: state.name });
            }}
          />

          <Button
            style={{ flexShrink: 0 }}
            onClick={() => window.open('/component/plugin/create')}
            iconType="spark-plus-line"
          >
            {$i18n.get({
              id: 'main.pages.App.components.PluginSelector.index.createPlugin',
              dm: '创建插件',
            })}
          </Button>
        </Flex>
      }
      {
        <span className={styles.topDesc}>
          {$i18n.get({
            id: 'main.pages.App.components.PluginSelector.index.publishedPluginSupportAddToSmartAgentWorkflowApp',
            dm: '已发布的插件支持添加到智能体/工作流应用中，可前往',
          })}
          &nbsp;
          <a target="_blank" href="/component/plugin">
            {$i18n.get({
              id: 'main.pages.App.components.PluginSelector.index.pluginList',
              dm: '插件列表',
            })}
          </a>
          &nbsp;
          {$i18n.get({
            id: 'main.pages.App.components.PluginSelector.index.manage',
            dm: '管理',
          })}
        </span>
      }
      <Flex vertical gap={12} className={classNames(styles.list)}>
        {state.loading ? (
          <Spin spinning />
        ) : !state.list.length ? (
          <Empty
            description={$i18n.get({
              id: 'main.pages.App.components.PluginSelector.index.noAvailablePlugin',
              dm: '暂无可用插件',
            })}
            style={{ marginTop: 90 }}
          />
        ) : (
          state.list.map((item) => (
            <PluginListItem
              key={item.plugin_id}
              item={item}
              selectedTools={value}
              onSelectTool={(tool) => handleAddTools([tool])}
              onRemoveTool={(tool) => handleRemove(tool)}
            ></PluginListItem>
          ))
        )}
      </Flex>
      <Pagination
        hideOnSinglePage
        style={{ justifyContent: 'flex-end', paddingTop: 12 }}
        pageSize={state.size}
        current={state.current}
        total={state.total}
        onChange={(current, size) => {
          setState({ current, size });
          fetchList({ current, size });
        }}
      />
    </Flex>
  );
}

export function ToolSelectorModal(props: {
  onClose: () => void;
  onOk?: (val: PluginTool[]) => void;
  value?: PluginTool[];
  maxLen?: number;
}) {
  const { maxLen = 1 } = props;
  const [value, setValue] = useSetState({
    selectedTools: (props.value || []) as PluginTool[],
  });

  return (
    <Modal
      width={880}
      onCancel={props.onClose}
      open
      title={$i18n.get({
        id: 'main.pages.App.components.PluginSelector.index.selectPlugin',
        dm: '选择插件',
      })}
      onOk={() => props.onOk?.(value.selectedTools)}
      okButtonProps={{
        disabled: value.selectedTools?.length > maxLen,
      }}
    >
      <div className="h-[400px] overflow-y-auto">
        <ToolSelector
          className={styles.modelCon}
          value={value.selectedTools}
          maxLength={maxLen}
          onChange={(val) =>
            setValue({
              selectedTools: val,
            })
          }
        />
      </div>
    </Modal>
  );
}

export function ToolSelectorDrawer(props: {
  onClose: () => void;
  onOk: (val: PluginTool[]) => void;
  value: Array<PluginTool>;
  maxLen?: number;
}) {
  const [selectedTools, setSelectedTools] = useState<PluginTool[]>(
    props.value || [],
  );
  const memoTitle = useMemo(() => {
    return (
      <Flex align="center" className={styles.drawerTitle} gap={4}>
        {$i18n.get({
          id: 'main.pages.App.components.PluginSelector.index.selectPlugin',
          dm: '选择插件',
        })}
      </Flex>
    );
  }, [props]);
  return (
    <Drawer
      className={styles.toolSelectDrawer}
      width={640}
      open
      onClose={props.onClose}
      title={memoTitle}
      footer={
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
          className="w-full"
        >
          <div
            style={{
              color: 'var(--ag-ant-color-text-tertiary)',
              fontSize: '14px',
              fontWeight: 'normal',
              lineHeight: '24px',
            }}
          >
            {!!selectedTools.length &&
              $i18n.get(
                {
                  id: 'main.pages.App.components.PluginSelector.index.addedToolVar1Var2',
                  dm: '已添加工具{var1}/{var2}',
                },
                { var1: selectedTools?.length || 0, var2: props.maxLen || 10 },
              )}
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              gap: 12,
            }}
          >
            <Button
              type="default"
              onClick={() => {
                props.onClose();
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.components.PluginSelector.index.cancel',
                dm: '取消',
              })}
            </Button>
            <Button
              type="primary"
              onClick={() => {
                props.onOk(selectedTools);
                props.onClose();
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.components.PluginSelector.index.addSuccess',
                    dm: '添加成功！',
                  }),
                );
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.components.PluginSelector.index.confirm',
                dm: '确认',
              })}
            </Button>
          </div>
        </div>
      }
    >
      <ToolSelector
        value={selectedTools}
        onChange={(val) => setSelectedTools(val)}
        className={styles.listWrap}
      />
    </Drawer>
  );
}
