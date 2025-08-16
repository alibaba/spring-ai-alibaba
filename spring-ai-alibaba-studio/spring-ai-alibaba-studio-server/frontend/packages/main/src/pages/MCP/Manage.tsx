import CardList from '@/components/Card/List';
import { useInnerLayout } from '@/components/InnerLayout/utils';
import $i18n from '@/i18n';
import {
  deleteMcpServer,
  listMcpServers,
  updateMcpServer,
} from '@/services/mcp';
import { IMcpServer, IPagingList, McpStatus } from '@/types/mcp';
import {
  AlertDialog,
  Button,
  ButtonProps,
  IconFont,
  message,
} from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { memo } from 'react';
import { useNavigate } from 'react-router-dom';
import { history } from 'umi';
import McpCard from './components/McpCard';
import styles from './Manage.module.less';

export const CreateMcpBtn = memo(
  (props: {
    buttonProps?: ButtonProps;
    text?: string;
    isOpenNew?: boolean;
  }) => {
    const handleCreateMcp = () => {
      if (props.isOpenNew) {
        window.open('/mcp/create');
      } else {
        history.push('/mcp/create');
      }
    };

    return (
      <>
        <Button
          onClick={handleCreateMcp}
          type="primary"
          icon={<IconFont type="spark-plus-line" />}
          {...props.buttonProps}
        >
          {props.text ||
            $i18n.get({
              id: 'main.pages.MCP.Manage.createMcpService',
              dm: '创建MCP服务',
            })}
        </Button>
      </>
    );
  },
);

export default function McpManage() {
  const { rightPortal } = useInnerLayout();
  const navigate = useNavigate();

  const [state, setState] = useSetState<{
    list: IMcpServer[];
    pageNo: number;
    pageSize: number;
    total: number;
    loading: boolean;
  }>({
    list: [],
    pageNo: 1,
    pageSize: 50,
    total: 0,
    loading: false,
  });

  const fetchList = async (
    extraParams = {} as Partial<{ pageNo: number; pageSize: number }>,
  ) => {
    setState({ loading: true });
    try {
      const queryParams = {
        current: extraParams.pageNo ?? state.pageNo,
        size: extraParams.pageSize ?? state.pageSize,
        need_tools: false,
      };

      const response = await listMcpServers(queryParams);
      if (response && response.data) {
        const pagingData = response.data as IPagingList<IMcpServer>;
        setState({
          list: pagingData.records || [],
          total: pagingData.total || 0,
          pageNo: queryParams.current,
          pageSize: queryParams.size,
        });
      }
    } finally {
      setState({ loading: false });
    }
  };

  useMount(() => {
    fetchList();
  });

  const handleConfirmDelete = (item: IMcpServer) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.MCP.Manage.confirmDeleteThisMcpService',
        dm: '确认删除此MCP服务吗',
      }),
      children: $i18n.get({
        id: 'main.pages.MCP.Manage.deleteWillNotBeRecoverableAlreadyAddedServicesMayFailPleaseProceedWithCaution',
        dm: '删除后将不可恢复，已经添加该服务的智能体可能会失效，请谨慎操作',
      }),

      danger: true,
      onOk: async () => {
        await deleteMcpServer(item.server_code);
        message.success(
          $i18n.get({
            id: 'main.pages.MCP.Manage.deletionSuccessful',
            dm: '删除成功',
          }),
        );
        fetchList();
      },
    });
  };

  const handleConfirmStop = (item: IMcpServer) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.MCP.Manage.confirmStopDeploymentOfThisService',
        dm: '确认停止部署此服务吗',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.MCP.Manage.cancel',
        dm: '取消',
      }),
      okText: $i18n.get({
        id: 'main.pages.MCP.Manage.stopDeployment',
        dm: '停止部署',
      }),
      onOk: async () => {
        await updateMcpServer({
          ...item,
          status: McpStatus.DISABLED,
        });
        message.success(
          $i18n.get({
            id: 'main.pages.MCP.Manage.stoppingSuccessful',
            dm: '停止成功',
          }),
        );
        fetchList();
      },
    });
  };

  const handleConfirmDeploy = (item: IMcpServer) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.MCP.Manage.confirmStartDeploymentOfThisService',
        dm: '确认启动部署此服务吗',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.MCP.Manage.cancel',
        dm: '取消',
      }),
      okText: $i18n.get({
        id: 'main.pages.MCP.Manage.startDeployment',
        dm: '启动部署',
      }),
      onOk: async () => {
        await updateMcpServer({
          ...item,
          status: McpStatus.ENABLED,
        });
        message.success(
          $i18n.get({
            id: 'main.pages.MCP.Manage.startingSuccessful',
            dm: '启动成功',
          }),
        );
        fetchList();
      },
    });
  };

  const handleAction = (action?: string, item?: IMcpServer) => {
    if (!action || !item) return;
    switch (action) {
      case 'delete':
        handleConfirmDelete(item);
        break;
      case 'start':
        handleConfirmDeploy(item);
        break;
      case 'stop':
        handleConfirmStop(item);
        break;
      case 'edit':
        navigate(`/mcp/edit/${item.server_code}`);
        break;
      case 'detail':
        navigate(`/mcp/detail/${item.server_code}`);
        break;
      default:
        navigate(`/mcp/detail/${item.server_code}`);
    }
  };

  const handlePageChange = (page: number, pageSize: number) => {
    fetchList({ pageNo: page, pageSize });
  };

  return (
    <div className={styles.container}>
      {state?.list?.length > 0 && rightPortal(<CreateMcpBtn />)}
      <CardList
        loading={state.loading}
        pagination={{
          current: state.pageNo,
          total: state.total,
          pageSize: state.pageSize,
          onChange: handlePageChange,
        }}
        emptyAction={<CreateMcpBtn />}
      >
        {state.list.map((item: IMcpServer) => (
          <McpCard key={item.server_code} data={item} onClick={handleAction} />
        ))}
      </CardList>
    </div>
  );
}
