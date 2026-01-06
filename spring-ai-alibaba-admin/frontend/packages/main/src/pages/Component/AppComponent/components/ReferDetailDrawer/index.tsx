import $i18n from '@/i18n';
import { getAppComponentRefApps, IAppType } from '@/services/appComponent';
import { IAppComponentListItem, IReferAppListItem } from '@/types/appComponent';
import { Drawer } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Table, Typography } from 'antd';
import { ColumnProps } from 'antd/es/table';
import { useMemo } from 'react';

interface IProps {
  data: IAppComponentListItem;
  onClose: () => void;
}

export default function ReferDetailDrawer(props: IProps) {
  const [state, setState] = useSetState({
    loading: true,
    list: [] as Array<IReferAppListItem>,
  });
  const columns: ColumnProps<IReferAppListItem>[] = useMemo(() => {
    return [
      {
        title: $i18n.get({
          id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.name',
          dm: '名称',
        }),
        dataIndex: 'name',
        render: (val) => (
          <Typography.Text style={{ width: 280 }} ellipsis={{ tooltip: val }}>
            {val}
          </Typography.Text>
        ),
      },
      {
        title: $i18n.get({
          id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.type',
          dm: '类型',
        }),
        dataIndex: 'type',
        width: 140,
        render: (type: IAppType) => {
          return type === IAppType.WORKFLOW
            ? $i18n.get({
                id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.workflow',
                dm: '工作流',
              })
            : $i18n.get({
                id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.smartAgentApplication',
                dm: '智能体应用',
              });
        },
      },
      {
        title: $i18n.get({
          id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.operation',
          dm: '操作',
        }),
        width: 90,
        dataIndex: 'action',
        render: (_, record) => {
          return (
            <a
              onClick={() => {
                window.open(
                  `/app/${
                    record.type === IAppType.WORKFLOW ? 'workflow' : 'assistant'
                  }/${record.app_id}`,
                );
              }}
            >
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.detail',
                dm: '详情',
              })}
            </a>
          );
        },
      },
    ];
  }, []);

  useMount(() => {
    getAppComponentRefApps(props.data.code!)
      .then((res) => {
        setState({ list: res, loading: false });
      })
      .catch(() => {
        setState({ loading: false });
      });
  });
  return (
    <Drawer
      width={640}
      onClose={props.onClose}
      open
      title={$i18n.get({
        id: 'main.pages.Component.AppComponent.components.ReferDetailDrawer.index.componentReferenceDetails',
        dm: '组件引用详情',
      })}
    >
      <Table
        rowKey={'code'}
        loading={state.loading}
        dataSource={state.list}
        pagination={false}
        columns={columns}
      />
    </Drawer>
  );
}
