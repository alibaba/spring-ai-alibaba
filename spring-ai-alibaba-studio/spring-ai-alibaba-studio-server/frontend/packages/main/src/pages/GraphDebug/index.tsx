import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import { useNavigate } from 'react-router-dom';
import GraphDebugCard from './components/GraphDebugCard';
import CreateModal from './components/CreateModal';
import { useMount, useSetState } from 'ahooks';
import graphDebugService from '@/services/graphDebugService';
import { IGraphCard } from '@/types/graph';

export default function () {
  const navigate = useNavigate();
  const [state, setState] = useSetState({
    loading: true,
    list: [] as IGraphCard[],
    showCreateModal: false,
    total: 0,
    current: 1,
    size: 20,
  });

  const fetchList = async () => {
    setState({ loading: true });
    try {
      const res = await graphDebugService.getGraphList({
        current: state.current,
        size: state.size,
        ownerID: 'saa', // 明确传递用户ID
      });
      setState({
        list: res.records,
        total: res.total,
        loading: false,
      });
    } catch (error) {
      setState({ 
        loading: false,
        list: [], // 清空列表
        total: 0 
      });
    }
  };

  useMount(() => {
    fetchList();
  });

  const handleCardClick = (graphId: string) => {
    navigate(`/graph-debug/${graphId}`);
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.GraphDebug.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.GraphDebug.index.graphDebug',
            dm: '图形调试',
          }),
        },
      ]}
      right={
        <Button
          onClick={() => setState({ showCreateModal: true })}
          icon={<IconFont type="spark-plus-line" />}
          type="primary"
        >
          {$i18n.get({
            id: 'main.pages.GraphDebug.index.createGraph',
            dm: '创建图',
          })}
        </Button>
      }
    >
      <div className="mx-[20px] my-[16px] grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {state.loading ? (
          <div className="col-span-full flex justify-center items-center h-64">
            <div className="text-center">
              <div className="loading-spinner mb-4"></div>
              <p>加载工作流列表中...</p>
            </div>
          </div>
        ) : state.list.length === 0 ? (
          <div className="col-span-full flex flex-col justify-center items-center h-64 text-center">
            <div className="mb-4 text-gray-400">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 12c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm9 2c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm7-2c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2z"/>
              </svg>
            </div>
            <h3 className="text-lg font-medium mb-2">暂无工作流</h3>
            <p className="text-gray-500 mb-4">
              还没有创建任何工作流，点击右上角创建按钮开始
            </p>
          </div>
        ) : (
          state.list.map((item) => (
            <GraphDebugCard
              key={item.id}
              {...item}
              onClick={() => handleCardClick(item.id)}
            />
          ))
        )}
      </div>

      {state.showCreateModal && (
        <CreateModal
          onCancel={() => setState({ showCreateModal: false })}
          onOk={(graph) => {
            setState({ showCreateModal: false });
            handleCardClick(graph.id);
          }}
        />
      )}
    </InnerLayout>
  );
}
