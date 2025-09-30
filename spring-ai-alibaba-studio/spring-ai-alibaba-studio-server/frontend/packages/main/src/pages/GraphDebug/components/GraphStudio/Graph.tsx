import React, { useState, useEffect, useRef } from 'react';
import { Button, Modal, Select, Checkbox, Typography } from 'antd';
import {
  BugOutlined,
  SelectOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined,
} from '@ant-design/icons';
import { IGraphData } from '@/types/graph';
import { GraphStudioEvent } from './index';
import {
  mockExecutionSteps,
} from '@/mock/graphmock';
import styles from './index.module.less';

const { Text } = Typography;
const { Option } = Select;

interface GraphProps {
  graphData: IGraphData;
  selectedNode: string | null;
  debugNodes: string[];
  onNodeClick: (nodeId: string) => void;
  onDebugConfig: (nodes: string[]) => void;
  dispatchEvent: (event: GraphStudioEvent) => void;
}

// Graph状态图可视化组件
const Graph: React.FC<GraphProps> = ({
  graphData,
  selectedNode,
  debugNodes,
  onNodeClick,
  onDebugConfig,
  dispatchEvent,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<any>(null);
  const [isNodeModalVisible, setIsNodeModalVisible] = useState(false);
  const [isDebugModalVisible, setIsDebugModalVisible] = useState(false);

  const [currentNodeInfo, setCurrentNodeInfo] = useState<any>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<string>('');
  const [tempDebugNodes, setTempDebugNodes] = useState<string[]>([]);

  // 当前缩放用于按钮展示
  const [currentZoom, setCurrentZoom] = useState(1);
  const [isRunning, setIsRunning] = useState(false);

  // 初始化 G6 图（通过 CDN 动态加载，避免本地安装依赖）
  useEffect(() => {
    if (!containerRef.current) return;

    const width = containerRef.current.clientWidth || 800;
    const height = containerRef.current.clientHeight || 600;

    const ensureG6 = async (): Promise<any> => {
      const w = window as any;
      if (w.G6) return w.G6;
      await new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.src = 'https://unpkg.com/@antv/g6@4.8.24/dist/g6.min.js';
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('G6 load failed'));
        document.body.appendChild(script);
      });
      return (window as any).G6;
    };

    let cleanup: (() => void) | undefined;

    const init = async () => {
      try {
        const G6 = await ensureG6();
        const graph = new G6.Graph({
          container: containerRef.current,
          width,
          height,
          modes: {
            default: ['drag-canvas', {
              type: 'zoom-canvas',
              sensitivity: 2,
              enableOptimizeZoom: true,
              minZoom: 0.2,
              maxZoom: 4,
            }, 'drag-node'],
          },
          defaultNode: {
            size: [120, 36],
            type: 'rect',
            style: {
              radius: 6,
              fill: '#f5f7fa',
              stroke: '#d9d9d9',
            },
            labelCfg: {
              style: { fill: '#262626', fontSize: 12 },
            },
          },
          nodeStateStyles: {
            executing: {
              fill: '#e6f7ff',
              stroke: '#1890ff',
              lineWidth: 1.5,
              shadowColor: '#1890ff',
              shadowBlur: 8,
            },
            selected: {
              fill: '#fff7e6',
              stroke: '#fa8c16',
              lineWidth: 2,
              shadowColor: '#fa8c16',
              shadowBlur: 6,
            },
          },
          defaultEdge: {
            type: 'quadratic',
            style: {
              endArrow: true,
              stroke: '#bfbfbf',
            },
            labelCfg: {
              autoRotate: true,
              style: { fill: '#8c8c8c', fontSize: 11 },
            },
          },
          animate: true,
        });

        // 设置数据
        const nodes = (graphData.nodes || []).map((n: any) => ({
          id: n.id,
          label: n.data?.label || n.name || n.id,
          type: n.data?.type === 'start' || n.data?.type === 'end' ? 'circle' : 'rect',
          size: n.data?.type === 'start' || n.data?.type === 'end' ? 40 : [120, 36],
          style: n.data?.type === 'start' || n.data?.type === 'end' ? { fill: '#fff', stroke: '#8c8c8c' } : undefined,
          x: n.position?.x || 100,
          y: n.position?.y || 100,
        }));

        // 处理平行边：当相同 source/target 存在多条连线时，自动设置偏移避免重叠
        const edges = (graphData.edges || []).map((e: any) => ({
          id: e.id,
          source: e.source,
          target: e.target,
          label: e.label,
        }));
        
        // 使用 G6 工具为平行边添加 curveOffset / type 等属性
        if (G6?.Util?.processParallelEdges) {
          G6.Util.processParallelEdges(edges as any, 16, 'quadratic', 'line', 'loop');
        }

        const data = { nodes, edges } as any;

        graph.data(data);
        graph.render();
        graph.fitView(20);
        setCurrentZoom(graph.getZoom());

        // 事件：节点点击
        graph.on('node:click', (evt: any) => {
          const nodeId = evt.item?.getID?.();
          if (!nodeId) return;
          const nodeInfo = graphData.nodes?.find((n: any) => n.id === nodeId);
          if (nodeInfo) {
            setCurrentNodeInfo(nodeInfo);
            setIsNodeModalVisible(true);
            onNodeClick(nodeId);
          }
        });

        // 事件：缩放更新
        graph.on('viewportchange', () => {
          setCurrentZoom(graph.getZoom());
        });

        graphRef.current = graph;

        // 处理尺寸变化
        const resizeObserver = new ResizeObserver(entries => {
          for (const entry of entries) {
            if (entry.target === containerRef.current) {
              const cw = entry.contentRect.width;
              const ch = entry.contentRect.height;
              graph.changeSize(cw, ch);
            }
          }
        });
        if (containerRef.current) {
          resizeObserver.observe(containerRef.current as Element);
        }

        cleanup = () => {
          resizeObserver.disconnect();
          graph.destroy();
          graphRef.current = null;
        };
      } catch (error) {
        console.error('Failed to initialize G6:', error);
      }
    };

    void init();

    return () => {
      if (cleanup) cleanup();
    };
  }, []);

  // 监听外部选中节点变化，高亮对应节点
  useEffect(() => {
    highlightSelectedNode(selectedNode);
  }, [selectedNode]);

  // 处理缩放
  const handleZoom = (direction: 'in' | 'out') => {
    const graph = graphRef.current;
    if (!graph) return;
    const factor = direction === 'in' ? 1.2 : 1 / 1.2;
    graph.zoom(factor);
    setCurrentZoom(graph.getZoom());
  };

  // 重置图形位置
  const handleResetPosition = () => {
    const graph = graphRef.current;
    if (!graph) return;
    graph.zoomTo(1);
    graph.fitView(20);
    setCurrentZoom(graph.getZoom());
  };

  // 高亮当前执行节点
  const highlightExecutingNode = (nodeId?: string) => {
    const graph = graphRef.current;
    if (!graph) return;
    const nodes = graph.getNodes();
    nodes.forEach((n: any) => graph.clearItemStates(n, 'executing'));
    if (nodeId) {
      const item = graph.findById(nodeId);
      if (item) {
        graph.setItemState(item, 'executing', true);
      }
    }
  };

  // 高亮选中节点（从右侧点击时）
  const highlightSelectedNode = (nodeId?: string | null) => {
    const graph = graphRef.current;
    if (!graph) return;
    const nodes = graph.getNodes();
    nodes.forEach((n: any) => graph.clearItemStates(n, 'selected'));
    if (nodeId) {
      const item = graph.findById(nodeId);
      if (item) {
        graph.setItemState(item, 'selected', true);
      }
    }
  };

  // 执行图（流式步骤）
  const handleRunGraph = async () => {
    if (isRunning) return;
    setIsRunning(true);
    try {
      for (const step of mockExecutionSteps) {
        highlightExecutingNode(step.nodeId);
        dispatchEvent({
          type: 'result',
          payload: {
            type: 'node-step',
            nodeId: step.nodeId,
            data: { input: step.input, response: step.response },
            summary: step.summary,
            timestamp: new Date().toISOString(),
          },
        });
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      // 完成后清除高亮
      highlightExecutingNode(undefined);
      // 通知完成
      dispatchEvent({
        type: 'result',
        payload: {
          type: 'run-completed',
          timestamp: new Date().toISOString(),
        },
      });
    } finally {
      setIsRunning(false);
    }
  };

  // 处理调试配置
  const handleDebugSubmit = () => {
    onDebugConfig(tempDebugNodes);
    setIsDebugModalVisible(false);
    setTempDebugNodes([]);
  };

  // 处理工作流选择
  const handleWorkflowSelect = (workflowId: string) => {
    setSelectedWorkflow(workflowId);
    dispatchEvent({
      type: 'graph-active',
      payload: { workflowId },
    });
  };

  // 处理节点中断设置
  const handleInterruptSetting = (type: 'before' | 'after') => {
    if (currentNodeInfo) {
      dispatchEvent({
        type: 'state-updated',
        payload: {
          nodeId: currentNodeInfo.id,
          interrupt: type,
        },
      });
      setIsNodeModalVisible(false);
    }
  };

  return (
    <div className={styles['graph-container']}>
      {/* 顶部浮动工具栏 */}
      <div className={styles['graph-top-toolbar']}>
        <Button
          size="small"
          icon={<BugOutlined />}
          onClick={() => setIsDebugModalVisible(true)}
        >
          调试
        </Button>

        <Select
          size="small"
          style={{ width: 200 }}
          placeholder="选择工作流"
          value={selectedWorkflow || undefined}
          onChange={handleWorkflowSelect}
        >
          <Option key="default" value="default" title="默认工作流">
            默认工作流
          </Option>
        </Select>

        <Button
          size="small"
          icon={<ZoomInOutlined />}
          onClick={() => handleZoom('in')}
        />

        <Button
          size="small"
          icon={<ZoomOutOutlined />}
          onClick={() => handleZoom('out')}
        />

        <Button
          size="small"
          icon={<FullscreenOutlined />}
          onClick={handleResetPosition}
          title="重置位置和缩放"
        />
      </div>

      {/* G6 图形展示区 */}
      <div
        ref={containerRef}
        className={styles['mermaid-container']}
        style={{ width: '100%', height: '100%' }}
      />

      {/* 底部浮动工具栏 */}
      <div className={styles['graph-bottom-toolbar']}>
        <Button
          size="small"
          icon={<SelectOutlined />}
        >
          展开输入
        </Button>

        <Button
          type="primary"
          size="small"
          loading={isRunning}
          onClick={handleRunGraph}
        >
          执行图
        </Button>
      </div>

      {/* 节点信息弹框 */}
      <Modal
        title="节点信息"
        open={isNodeModalVisible}
        onCancel={() => setIsNodeModalVisible(false)}
        footer={null}
        className={styles['node-modal']}
      >
        {currentNodeInfo && (
          <div>
            <div className={styles['node-info']}>
              <p><Text strong>节点ID:</Text> {currentNodeInfo.id}</p>
              <p><Text strong>节点名称:</Text> {currentNodeInfo.name}</p>
              <p><Text strong>节点类型:</Text> {currentNodeInfo.type}</p>
            </div>

            <div className={styles['node-actions']}>
              <Button
                onClick={() => handleInterruptSetting('before')}
              >
                Interrupt Before
              </Button>
              <Button
                onClick={() => handleInterruptSetting('after')}
              >
                Interrupt After
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* 调试配置弹框 */}
      <Modal
        title="调试配置"
        open={isDebugModalVisible}
        onOk={handleDebugSubmit}
        onCancel={() => setIsDebugModalVisible(false)}
        className={styles['debug-modal']}
      >
        <div className={styles['debug-nodes']}>
          <Text>选择要调试的节点:</Text>
          <div style={{ marginTop: 12 }}>
            {(graphData.nodes || []).map((node: any) => (
              <div key={node.id} style={{ marginBottom: 8 }}>
                <Checkbox
                  checked={tempDebugNodes.includes(node.id)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setTempDebugNodes([...tempDebugNodes, node.id]);
                    } else {
                      setTempDebugNodes(tempDebugNodes.filter(id => id !== node.id));
                    }
                  }}
                >
                  {node.data?.label || node.name || node.id} ({node.id})
                </Checkbox>
              </div>
            ))}
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Graph;
