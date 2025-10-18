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
      });
      setState({
        list: res.records,
        total: res.total,
        loading: false,
      });
    } catch (error) {
      console.error('Failed to fetch graph list:', error);
      setState({ loading: false });
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
        {state.list.map((item) => (
          <GraphDebugCard
            key={item.id}
            {...item}
            onClick={() => handleCardClick(item.id)}
          />
        ))}
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
