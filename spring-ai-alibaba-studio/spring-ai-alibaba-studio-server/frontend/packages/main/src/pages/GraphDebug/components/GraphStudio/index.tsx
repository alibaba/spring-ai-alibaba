import React, { useState, useEffect, useRef } from 'react';
import { IGraphData } from '@/types/graph';
import Graph from './Graph';
import Executor from './Executor';
import Result from './Result';
import styles from './index.module.less';

interface GraphStudioProps {
  graphData: IGraphData;
}

// 自定义事件类型
export interface GraphStudioEvent {
  type: 'init' | 'result' | 'graph-active' | 'state-updated' | 'node-selected' | 'debug-request';
  payload?: any;
}

// GraphStudio主布局容器组件
export const GraphStudio: React.FC<GraphStudioProps> = ({ graphData }) => {
  const workbenchRef = useRef<HTMLDivElement>(null);

  // 全局状态管理
  const [currentThread, setCurrentThread] = useState<string>('');
  const [executionResults, setExecutionResults] = useState<any[]>([]);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [debugNodes, setDebugNodes] = useState<string[]>([]);
  const [isExecutorExpanded, setIsExecutorExpanded] = useState(false);

  // 事件系统 - 组件间通信
  const dispatchEvent = (event: GraphStudioEvent) => {
    const customEvent = new CustomEvent('graph-studio-event', {
      detail: event,
    });
    workbenchRef.current?.dispatchEvent(customEvent);
  };

  // 监听自定义事件
  useEffect(() => {
    const handleEvent = (e: CustomEvent<GraphStudioEvent>) => {
      const { type, payload } = e.detail;

      switch (type) {
        case 'init':
          break;

        case 'result':
          setExecutionResults(prev => [...prev, payload]);
          break;

        case 'graph-active':
          break;

        case 'state-updated':
          break;

        case 'node-selected':
          setSelectedNode(payload.nodeId);
          break;

        case 'debug-request':
          setDebugNodes(payload.nodes);
          break;

        default:
          break;
      }
    };

    const workbenchElement = workbenchRef.current;
    if (workbenchElement) {
      workbenchElement.addEventListener('graph-studio-event', handleEvent as EventListener);
      return () => {
        workbenchElement.removeEventListener('graph-studio-event', handleEvent as EventListener);
      };
    }
    return undefined;
  }, []);

  // 处理线程选择
  const handleThreadSelect = (threadId: string) => {
    setCurrentThread(threadId);
    dispatchEvent({
      type: 'state-updated',
      payload: { currentThread: threadId },
    });
  };

  // 处理执行器展开/收起
  const handleExecutorToggle = () => {
    setIsExecutorExpanded(!isExecutorExpanded);
  };

  // 处理表单提交
  const handleSubmit = (formData: any) => {
    dispatchEvent({
      type: 'init',
      payload: { formData, graphData },
    });
  };

  // 处理节点点击
  const handleNodeClick = (nodeId: string) => {
    dispatchEvent({
      type: 'node-selected',
      payload: { nodeId },
    });
  };

  // 处理调试设置
  const handleDebugConfig = (nodes: string[]) => {
    dispatchEvent({
      type: 'debug-request',
      payload: { nodes },
    });
  };

  return (
    <div ref={workbenchRef} className={styles.workbench}>
      {/* Graph 可视化展示区 */}
      <div className={styles['graph-visualization-area']}>
        <Graph
          graphData={graphData}
          selectedNode={selectedNode}
          debugNodes={debugNodes}
          onNodeClick={handleNodeClick}
          onDebugConfig={handleDebugConfig}
          dispatchEvent={dispatchEvent}
        />

        <Executor
          graphData={graphData}
          isExpanded={isExecutorExpanded}
          onToggle={handleExecutorToggle}
          onSubmit={handleSubmit}
          dispatchEvent={dispatchEvent}
        />
      </div>

      {/* 工作流执行详细结果展示区 */}
      <div className={styles['result-area']}>
        <Result
          currentThread={currentThread}
          executionResults={executionResults}
          onThreadSelect={handleThreadSelect}
          dispatchEvent={dispatchEvent}
          selectedNode={selectedNode}
        />
      </div>
    </div>
  );
};

export default GraphStudio;
