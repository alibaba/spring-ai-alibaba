import Filter from '@/components/Filter';
import $i18n from '@/i18n';
import { APP_ICON_IMAGE } from '@/pages/Component/AppComponent/components/AppSelector';
import { getAppComponentList, IAppType } from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { Empty, IconFont, Modal } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import {
  Button,
  Flex,
  Input,
  message,
  Pagination,
  Radio,
  Spin,
  Typography,
} from 'antd';
import classNames from 'classnames';
import { useMemo, useRef } from 'react';
import styles from './index.module.less';

interface IComponentSelectorModalProps {
  onClose: () => void;
  onOk: (item: IAppComponentListItem) => void;
  appCode: string;
}

const tabs = [
  {
    value: IAppType.AGENT,
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.intelligentAgentComponent',
      dm: '智能体组件',
    }),
  },
  {
    value: IAppType.WORKFLOW,
    label: $i18n.get({
      id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.workflowComponent',
      dm: '工作流组件',
    }),
  },
];

export default function ComponentSelectorModal(
  props: IComponentSelectorModalProps,
) {
  const [state, setState] = useSetState({
    activeTab: IAppType.AGENT,
    loading: true,
    list: [] as IAppComponentListItem[],
    total: 0,
    size: 10,
    current: 1,
    name: '',
    selectedComponent: null as IAppComponentListItem | null,
  });

  const isSearch = useRef(false);

  const fetchList = (params: any = {}) => {
    setState({ loading: true });
    const extraParams = isSearch.current ? { name: state.name } : {};
    getAppComponentList({
      current: state.current,
      size: state.size,
      type: state.activeTab,
      appCode: props.appCode,
      ...params,
      ...extraParams,
    }).then((res) => {
      setState({
        list: res.records,
        total: res.total,
        loading: false,
      });
    });
  };

  const handleSearch = () => {
    isSearch.current = true;
    setState({ current: 1 });
    fetchList({ current: 1 });
  };

  useMount(() => {
    fetchList();
  });

  const handleOk = () => {
    if (!state.selectedComponent) {
      message.warning(
        $i18n.get({
          id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.selectComponent',
          dm: '请选择组件',
        }),
      );
      return;
    }
    props.onOk(state.selectedComponent);
  };

  const typeName = useMemo(() => {
    return state.activeTab === IAppType.AGENT
      ? $i18n.get({
          id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.intelligent',
          dm: '智能体',
        })
      : $i18n.get({
          id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.workflow',
          dm: '工作流',
        });
  }, [state.activeTab]);

  const clickCreate = () => {
    window.open(
      `#/component-manage/${
        state.activeTab === IAppType.AGENT ? 'agent' : 'workflow'
      }`,
    );
  };

  return (
    <Modal
      onOk={handleOk}
      open
      width={900}
      onCancel={props.onClose}
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.selectAppComponent',
        dm: '选择应用组件',
      })}
      bodyProps={{ style: { padding: 0 } }}
    >
      <Flex className="py-[18px]" vertical gap={12}>
        <div className="px-[24px]">
          <Filter
            value={state.activeTab}
            onSelect={(val) => {
              isSearch.current = false;
              setState({ activeTab: val as IAppType, name: '', current: 1 });
              fetchList({ current: 1, type: val });
            }}
            options={tabs}
          />
        </div>
        <Flex className="px-[24px]" justify="space-between">
          <Input.Search
            className={styles.search}
            value={state.name}
            onChange={(e) => setState({ name: e.target.value })}
            onSearch={handleSearch}
            placeholder={$i18n.get({
              id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.searchComponentName',
              dm: '搜索组件名称',
            })}
          />

          <Button
            icon={<IconFont type="spark-plus-line" />}
            onClick={clickCreate}
          >
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.add',
              dm: '添加',
            })}

            {typeName}
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.component',
              dm: '组件',
            })}
          </Button>
        </Flex>
        <div className={styles['list-wrap']}>
          {state.loading ? (
            <Spin className="loading-center" spinning />
          ) : !state.list.length ? (
            <div className={styles['empty-wrap']}>
              <Empty
                title={$i18n.get(
                  {
                    id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.noComponent',
                    dm: '暂无{var1}组件',
                  },
                  { var1: typeName },
                )}
                description={$i18n.get({
                  id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.index.goToComponentManagementPageCreate',
                  dm: '请前往组件管理页面进行组件创建',
                })}
              >
                {!isSearch.current && (
                  <Button type="primary" className="mt-4" onClick={clickCreate}>
                    {$i18n.get({
                      id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.goToCreate',
                      dm: '前往创建',
                    })}
                  </Button>
                )}
              </Empty>
            </div>
          ) : (
            <>
              <Flex className={styles.list} align="flex-start" gap={12} wrap>
                {state.list.map((item) => {
                  const isActive = state.selectedComponent?.code === item.code;
                  return (
                    <div
                      onClick={() => {
                        if (isActive) return;
                        setState({ selectedComponent: item });
                      }}
                      className={classNames(styles.card, {
                        [styles.selected]: isActive,
                      })}
                      key={item.code}
                    >
                      <Flex gap={8} align="center">
                        <img src={APP_ICON_IMAGE[item.type!]} alt="" />
                        <Flex align="center" gap={4} className="flex-1 w-1">
                          <Typography.Text
                            ellipsis={{ tooltip: item.name }}
                            className={styles.title}
                          >
                            {item.name}
                          </Typography.Text>
                        </Flex>
                        <Radio checked={isActive} />
                      </Flex>
                      <Typography.Text
                        ellipsis={{ tooltip: item.code }}
                        className={styles.desc}
                      >
                        {$i18n.get(
                          {
                            id: 'main.pages.App.Workflow.components.ComponentSelectorModal.index.id',
                            dm: 'ID：{var1}',
                          },
                          { var1: item.code },
                        )}
                      </Typography.Text>
                    </div>
                  );
                })}
              </Flex>
            </>
          )}
        </div>
        <div className="px-[24px]">
          <Pagination
            pageSize={state.size}
            total={state.total}
            hideOnSinglePage
            current={state.current}
            onChange={(current, size) => {
              setState({ current, size });
              fetchList({ current, size });
            }}
          />
        </div>
      </Flex>
    </Modal>
  );
}
