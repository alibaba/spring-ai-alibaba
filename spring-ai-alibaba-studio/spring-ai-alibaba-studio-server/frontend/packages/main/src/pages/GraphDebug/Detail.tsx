import { useEffect, useState } from 'react';
import { Spin, Empty, message } from 'antd';
import { useParams } from 'react-router-dom';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { GraphStudio } from './components/GraphStudio';
import { IGraphData } from '@/types/graph';
import graphDebugService from '@/services/graphDebugService';

type Params = {
  appId?: string;
};

export default function GraphDebugDetail() {
  const params = useParams<Params>();
  const [graphData, setGraphData] = useState<IGraphData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (params.appId) {
          // 使用新的API服务加载图数据
          const data = await graphDebugService.getGraphById(params.appId);
          setGraphData(data);
        } else {
          message.error('缺少图形ID参数');
        }
      } catch (error) {
        console.error('Failed to fetch graph data:', error);
        message.error('加载图数据失败');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [params.appId]);

  if (loading) {
    return (
      <InnerLayout
        breadcrumbLinks={[
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.home',
              dm: '首页',
            }),
            path: '/',
          },
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.graphDebug',
              dm: '图形调试',
            }),
            path: '/graph-debug',
          },
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.debugging',
              dm: '调试中',
            }),
          },
        ]}
        fullScreen
      >
        <div className="flex justify-center items-center h-full">
          <Spin tip="Loading Graph Studio..." size="large">
            <div className="w-32 h-32" />
          </Spin>
        </div>
      </InnerLayout>
    );
  }

  if (!graphData) {
    return (
      <InnerLayout
        breadcrumbLinks={[
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.home',
              dm: '首页',
            }),
            path: '/',
          },
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.graphDebug',
              dm: '图形调试',
            }),
            path: '/graph-debug',
          },
          {
            title: $i18n.get({
              id: 'main.pages.GraphDebug.Detail.notFound',
              dm: '未找到',
            }),
          },
        ]}
      >
        <div className="flex justify-center items-center h-full">
          <Empty
            description="未找到图数据"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </div>
      </InnerLayout>
    );
  }

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.GraphDebug.Detail.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.GraphDebug.Detail.graphDebug',
            dm: '图形调试',
          }),
          path: '/graph-debug',
        },
        {
          title: graphData.name,
        },
      ]}
      fullScreen
    >
      <GraphStudio graphData={graphData} />
    </InnerLayout>
  );
}
