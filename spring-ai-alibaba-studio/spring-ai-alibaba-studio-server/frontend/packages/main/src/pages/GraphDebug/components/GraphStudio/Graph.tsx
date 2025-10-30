import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Button, Modal, Select, Checkbox, Typography, message, Input, Form } from 'antd';
import {
  BugOutlined,
  SelectOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FullscreenOutlined,
  StopOutlined,
} from '@ant-design/icons';
import ReactFlow, {
  Node,
  Edge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  EdgeTypes,
  MarkerType,
  ReactFlowProvider,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { IGraphData } from '@/types/graph';
import { GraphStudioEvent } from './index';
import { nodeTypes } from './CustomNodes';
import graphDebugService from '@/services/graphDebugService';
import styles from './index.module.less';

const { Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface GraphProps {
  graphData: IGraphData;
  selectedNode: string | null;
  debugNodes: string[];
  onNodeClick: (nodeId: string) => void;
  onDebugConfig: (nodes: string[]) => void;
  dispatchEvent: (event: GraphStudioEvent) => void;
}

// Graph状态图可视化组件（内部实现）
const GraphInner: React.FC<GraphProps> = ({
  graphData,
  selectedNode,
  debugNodes,
  onNodeClick,
  onDebugConfig,
  dispatchEvent,
}) => {
  const [isNodeModalVisible, setIsNodeModalVisible] = useState(false);
  const [isDebugModalVisible, setIsDebugModalVisible] = useState(false);
  const [isInputModalVisible, setIsInputModalVisible] = useState(false);
  const [currentNodeInfo, setCurrentNodeInfo] = useState<any>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<string>('');
  const [tempDebugNodes, setTempDebugNodes] = useState<string[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [cleanupFn, setCleanupFn] = useState<(() => void) | null>(null);
  const [streamType, setStreamType] = useState<'enhanced' | 'basic' | 'snapshots'>('enhanced');
  const [executionStatus, setExecutionStatus] = useState<string>('');
  const [inputForm] = Form.useForm();
  // 节点执行状态映射：nodeId -> execution_status
  const [nodeExecutionStates, setNodeExecutionStates] = useState<Map<string, string>>(new Map());

  // 自动布局算法：从上到下排列节点
  const calculateLayout = useCallback((nodes: any[], edges: any[]): Map<string, { x: number; y: number }> => {
    if (!nodes || nodes.length === 0) return new Map();

    // 识别START和END节点
    const startNodes = nodes.filter(n => 
      n.id === '__START__' || n.type === 'start' || n.data?.type === 'start'
    );
    const endNodes = nodes.filter(n => 
      n.id === '__END__' || n.type === 'end' || n.data?.type === 'end'
    );

    // 构建邻接表
    const adjacencyList = new Map<string, string[]>();
    const inDegree = new Map<string, number>();
    
    nodes.forEach(node => {
      adjacencyList.set(node.id, []);
      inDegree.set(node.id, 0);
    });
    
    edges.forEach(edge => {
      if (adjacencyList.has(edge.source)) {
        adjacencyList.get(edge.source)!.push(edge.target);
      }
      inDegree.set(edge.target, (inDegree.get(edge.target) || 0) + 1);
    });

    // 拓扑排序分层
    const levels: string[][] = [];
    const nodeLevel = new Map<string, number>();
    const queue: string[] = [];
    
    // 优先处理START节点，确保它们在第0层
    if (startNodes.length > 0) {
      startNodes.forEach(node => {
        queue.push(node.id);
        nodeLevel.set(node.id, 0);
      });
    } else {
      // 如果没有明确的START节点，找到所有入度为0的节点
      inDegree.forEach((degree, nodeId) => {
        if (degree === 0 && !endNodes.some(n => n.id === nodeId)) {
          queue.push(nodeId);
          nodeLevel.set(nodeId, 0);
        }
      });
    }

    // BFS遍历计算每个节点的层级
    while (queue.length > 0) {
      const currentLevelSize = queue.length;
      const currentLevel: string[] = [];
      
      for (let i = 0; i < currentLevelSize; i++) {
        const nodeId = queue.shift()!;
        const level = nodeLevel.get(nodeId)!;
        currentLevel.push(nodeId);
        
        // 确保levels数组有足够的长度
        while (levels.length <= level) {
          levels.push([]);
        }
        levels[level].push(nodeId);
        
        // 处理所有子节点
        const neighbors = adjacencyList.get(nodeId) || [];
        neighbors.forEach(neighbor => {
          const currentInDegree = inDegree.get(neighbor)! - 1;
          inDegree.set(neighbor, currentInDegree);
          
          // 更新子节点的层级（取最大值以确保在所有父节点之后）
          const newLevel = level + 1;
          const existingLevel = nodeLevel.get(neighbor);
          if (existingLevel === undefined || newLevel > existingLevel) {
            nodeLevel.set(neighbor, newLevel);
          }
          
          if (currentInDegree === 0) {
            queue.push(neighbor);
          }
        });
      }
    }
    
    // 处理可能存在的孤立节点
    nodes.forEach(node => {
      if (!nodeLevel.has(node.id)) {
        const lastLevel = levels.length;
        nodeLevel.set(node.id, lastLevel);
        while (levels.length <= lastLevel) {
          levels.push([]);
        }
        levels[lastLevel].push(node.id);
      }
    });

    // 特殊处理：确保END节点在最后一层
    if (endNodes.length > 0) {
      const maxLevel = Math.max(...Array.from(nodeLevel.values()));
      const finalLevel = maxLevel + 1;
      
      endNodes.forEach(endNode => {
        // 从原来的层级中移除END节点
        const oldLevel = nodeLevel.get(endNode.id);
        if (oldLevel !== undefined && levels[oldLevel]) {
          levels[oldLevel] = levels[oldLevel].filter(id => id !== endNode.id);
        }
        
        // 将END节点放到最后一层
        nodeLevel.set(endNode.id, finalLevel);
        while (levels.length <= finalLevel) {
          levels.push([]);
        }
        if (!levels[finalLevel].includes(endNode.id)) {
          levels[finalLevel].push(endNode.id);
        }
      });
    }

    // 布局参数
    const horizontalSpacing = 250; // 水平间距
    const verticalSpacing = 150;   // 垂直间距
    const startY = -100;            // 起始Y坐标
    
    // 计算每个节点的位置
    const positions = new Map<string, { x: number; y: number }>();
    
    levels.forEach((level, levelIndex) => {
      const y = startY + levelIndex * verticalSpacing;
      const levelWidth = (level.length - 1) * horizontalSpacing;
      const startX = -levelWidth / 2; // 居中对齐
      
      level.forEach((nodeId, indexInLevel) => {
        const x = startX + indexInLevel * horizontalSpacing;
        positions.set(nodeId, { x, y });
      });
    });
    
    return positions;
  }, []);

  // 转换GraphData为ReactFlow的nodes和edges
  const initialNodes: Node[] = useMemo(() => {
    const rawNodes = (graphData.nodes || []).map((n: any) => {
      // 检测特殊节点类型
      const isStartNode = n.id === '__START__' || n.type === 'start' || n.data?.type === 'start';
      const isEndNode = n.id === '__END__' || n.type === 'end' || n.data?.type === 'end';
      const nodeType = n.data?.type || n.type || 'standard';
      
      let type = 'standard';
      if (isStartNode) type = 'start';
      else if (isEndNode) type = 'end';
      else if (nodeType === 'ai') type = 'ai';
      else if (nodeType === 'processor') type = 'processor';

      return {
        id: n.id,
        type,
        data: {
          ...n.data,
          label: n.data?.label || n.name || n.id.replace(/__/g, '').replace(/_/g, ' '),
          name: n.name,
          type: nodeType,
          executionStatus: null,
        },
      };
    });

    // 计算自动布局
    const positions = calculateLayout(graphData.nodes || [], graphData.edges || []);
    
    // 应用位置
    return rawNodes.map(node => ({
      ...node,
      position: positions.get(node.id) || { x: 0, y: 0 },
    }));
  }, [graphData.nodes, graphData.edges, calculateLayout]);

  const initialEdges: Edge[] = useMemo(() => {
    return (graphData.edges || []).map((e: any) => {
      const isConditionalEdge = e.type === 'conditional' || e.label;
      

      let edgeType = 'default'; // 默认使用贝塞尔曲线（bezier），最灵活美观
      
      if (e.edgeType) {
        // 优先使用后端指定的边类型
        edgeType = e.edgeType;
      } else if (isConditionalEdge) {
        edgeType = 'default';
      }
      
      return {
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle,
        targetHandle: e.targetHandle,
        label: e.label,
        type: edgeType,
        animated: false, // 可以根据需要开启动画效果
        style: {
          stroke: isConditionalEdge ? '#faad14' : '#bfbfbf',
          strokeWidth: isConditionalEdge ? 2 : 1.5,
          strokeDasharray: isConditionalEdge ? '5 5' : undefined,
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          width: 20,
          height: 20,
          color: isConditionalEdge ? '#faad14' : '#bfbfbf',
        },
        labelStyle: {
          fill: isConditionalEdge ? '#faad14' : '#8c8c8c',
          fontWeight: 500,
          fontSize: 12,
        },
        labelBgStyle: {
          fill: '#fff',
          fillOpacity: 0.8,
        },
      };
    });
  }, [graphData.edges]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // 更新节点数据
  useEffect(() => {
    setNodes(initialNodes);
  }, [initialNodes, setNodes]);

  useEffect(() => {
    setEdges(initialEdges);
  }, [initialEdges, setEdges]);

  // 监听外部选中节点变化
  useEffect(() => {
    setNodes((nds) =>
      nds.map((node) => ({
        ...node,
        data: {
          ...node.data,
          selected: node.id === selectedNode,
        },
      }))
    );
  }, [selectedNode, setNodes]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      if (cleanupFn) {
        cleanupFn();
      }
    };
  }, [cleanupFn]);

  // 连接节点回调
  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // 节点点击事件
  const onNodeClickHandler = useCallback(
    (event: React.MouseEvent, node: Node) => {
      const nodeInfo = graphData.nodes?.find((n: any) => n.id === node.id);
      if (nodeInfo) {
        setCurrentNodeInfo(nodeInfo);
        setIsNodeModalVisible(true);
        onNodeClick(node.id);
      }
    },
    [graphData.nodes, onNodeClick]
  );

  // 更新节点执行状态
  const updateNodeExecutionStatus = useCallback((nodeId: string, executionStatus: string) => {
    setNodeExecutionStates(prev => {
      const newMap = new Map(prev);
      newMap.set(nodeId, executionStatus);
      return newMap;
    });
  }, []);

  // 清除所有节点执行状态
  const clearNodeExecutionStates = useCallback(() => {
    setNodeExecutionStates(new Map());
  }, []);

  // 根据节点执行状态映射更新节点数据
  useEffect(() => {
    setNodes((nds) =>
      nds.map((node) => ({
        ...node,
        data: {
          ...node.data,
          executionStatus: nodeExecutionStates.get(node.id) || null,
        },
      }))
    );
  }, [nodeExecutionStates, setNodes]);

  // 执行图（流式步骤）- 真实调用后端API
  const handleRunGraph = async (customInputText?: string) => {
    if (isRunning) return;
    
    // 清理之前的执行
    if (cleanupFn) {
      cleanupFn();
      setCleanupFn(null);
    }
    
    // 清除所有节点的执行状态
    clearNodeExecutionStates();
    
    setIsRunning(true);
    setExecutionStatus('正在初始化...');
    
    try {
      // 使用自定义输入文本或默认文本
      const inputText = customInputText || '测试图执行';
      
      
      let cleanup: () => void;

      // 根据选择的流式类型调用不同的API
      switch (streamType) {
        case 'basic':
          setExecutionStatus('连接基础节点输出流...');
          cleanup = await graphDebugService.executeGraphBasic(
            graphData.id,
            inputText,
            (nodeOutput: any) => {
              const nodeId = nodeOutput.node;
              setExecutionStatus(`执行节点: ${nodeId}`);
              
              // 基础流暂时没有明确的状态，默认标记为执行中
              updateNodeExecutionStatus(nodeId, 'EXECUTING');
              
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'node_update',
                  streamType: 'basic',
                  data: nodeOutput,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              message.error('基础流执行过程中出现错误');
              setExecutionStatus('执行失败');
              setIsRunning(false);
              setCleanupFn(null);
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'basic',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 基础流执行完成');
              setExecutionStatus('执行完成');
              setIsRunning(false);
              setCleanupFn(null);
            }
          );
          break;

        case 'snapshots':
          setExecutionStatus('连接节点状态快照流...');
          cleanup = await graphDebugService.executeGraphSnapshots(
            graphData.id,
            inputText,
            (snapshot: any) => {
              const keys = Object.keys(snapshot).slice(0, 3).join(', ');
              setExecutionStatus(`接收状态快照: ${keys}...`);
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'state_update',
                  streamType: 'snapshots',
                  data: snapshot,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              message.error('快照流执行过程中出现错误');
              setExecutionStatus('执行失败');
              setIsRunning(false);
              setCleanupFn(null);
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'snapshots',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 快照流执行完成');
              setExecutionStatus('执行完成');
              setIsRunning(false);
              setCleanupFn(null);
            }
          );
          break;

        case 'enhanced':
        default:
          setExecutionStatus('连接增强节点输出流...');
          cleanup = await graphDebugService.executeGraphEnhanced(
            graphData.id,
            inputText,
            (nodeOutput: any) => {
              const nodeId = nodeOutput.node_id;
              const status = nodeOutput.execution_status || 'EXECUTING';
              
              setExecutionStatus(`${nodeId}: ${status}`);
              
              // 更新节点执行状态
              updateNodeExecutionStatus(nodeId, status);
              
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'node_update',
                  streamType: 'enhanced',
                  data: nodeOutput,
                  timestamp: new Date().toISOString(),
                },
              });
            },
            (error) => {
              message.error('增强流执行过程中出现错误');
              setExecutionStatus('执行失败');
              setIsRunning(false);
              setCleanupFn(null);
            },
            () => {
              dispatchEvent({
                type: 'result',
                payload: {
                  type: 'execution_complete',
                  streamType: 'enhanced',
                  data: { graphId: graphData.id, inputText },
                  timestamp: new Date().toISOString(),
                },
              });
              message.success('✅ 增强流执行完成');
              setExecutionStatus('执行完成');
              setIsRunning(false);
              setCleanupFn(null);
            }
          );
          break;
      }

      // 保存清理函数
      setCleanupFn(() => cleanup);

      // 触发初始事件
      dispatchEvent({
        type: 'result',
        payload: {
          type: 'execution_start',
          streamType: streamType,
          data: { 
            graphId: graphData.id, 
            graphName: graphData.name,
            inputText,
          },
          timestamp: new Date().toISOString(),
        },
      });

      setExecutionStatus(`执行中 (${streamType})...`);
      message.success(`🚀 开始执行图工作流 (${streamType})`);
    } catch (error) {
      message.error(`执行失败: ${error instanceof Error ? error.message : '未知错误'}`);
      setExecutionStatus('执行失败');
      setIsRunning(false);
      setCleanupFn(null);
    }
  };

  // 停止执行
  const handleStopExecution = () => {
    if (cleanupFn) {
      cleanupFn();
      setCleanupFn(null);
      setIsRunning(false);
      setExecutionStatus('已停止');
      message.warning('执行已停止');
    }
  };

  // 打开输入框
  const handleOpenInputModal = () => {
    setIsInputModalVisible(true);
    // 设置默认值
    inputForm.setFieldsValue({
      inputText: '请输入要处理的文本内容...',
    });
  };

  // 提交输入并执行
  const handleSubmitInput = async () => {
    try {
      const values = await inputForm.validateFields();
      const inputText = values.inputText?.trim();
      
      if (!inputText) {
        message.warning('请输入文本内容');
        return;
      }

      // 关闭弹窗
      setIsInputModalVisible(false);
      
      // 执行图
      await handleRunGraph(inputText);
    } catch (error) {
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

        <Select
          size="small"
          style={{ width: 180 }}
          value={streamType}
          onChange={setStreamType}
          disabled={isRunning}
        >
          <Option value="enhanced">增强流 (Enhanced)</Option>
          <Option value="basic">基础流 (Basic)</Option>
          <Option value="snapshots">快照流 (Snapshots)</Option>
        </Select>

        {executionStatus && (
          <Text 
            type={isRunning ? 'secondary' : 'success'} 
            style={{ fontSize: '12px', marginLeft: '8px' }}
          >
            {executionStatus}
          </Text>
        )}
      </div>

      {/* React Flow 图形展示区 */}
      <div className={styles['react-flow-wrapper']} style={{ width: '100%', height: '100%' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={onNodeClickHandler}
          nodeTypes={nodeTypes}
          fitView
          attributionPosition="bottom-left"
        >
          <Background color="#aaa" gap={16} />
          <Controls />
          <MiniMap
            nodeColor={(node) => {
              const executionStatus = node.data?.executionStatus;
              if (executionStatus === 'EXECUTING') return '#1890ff';
              if (executionStatus === 'SUCCESS') return '#52c41a';
              if (executionStatus === 'FAILED') return '#ff4d4f';
              if (executionStatus === 'SKIPPED') return '#d9d9d9';
              if (node.data?.selected) return '#fa8c16';
              if (node.type === 'start') return '#52c41a';
              if (node.type === 'end') return '#ff4d4f';
              return '#f5f7fa';
            }}
            style={{
              backgroundColor: '#fff',
              border: '1px solid #d9d9d9',
            }}
          />
        </ReactFlow>
      </div>

      {/* 底部浮动工具栏 */}
      <div className={styles['graph-bottom-toolbar']}>
        <Button
          size="small"
          icon={<SelectOutlined />}
          onClick={handleOpenInputModal}
          disabled={isRunning}
        >
          展开输入
        </Button>

        {isRunning ? (
          <Button
            danger
            size="small"
            icon={<StopOutlined />}
            onClick={handleStopExecution}
          >
            停止执行
          </Button>
        ) : (
          <Button
            type="primary"
            size="small"
            onClick={() => handleRunGraph()}
          >
            执行图
          </Button>
        )}
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

      {/* 输入文本弹框 */}
      <Modal
        title="输入执行参数"
        open={isInputModalVisible}
        onOk={handleSubmitInput}
        onCancel={() => setIsInputModalVisible(false)}
        okText="执行"
        cancelText="取消"
        width={600}
      >
        <Form
          form={inputForm}
          layout="vertical"
        >
          <Form.Item
            name="inputText"
            label="输入文本"
            rules={[{ required: true, message: '请输入文本内容' }]}
          >
            <TextArea
              rows={6}
              placeholder="请输入要处理的文本内容..."
            />
          </Form.Item>

          <Form.Item
            label="流式类型"
          >
            <Select
              value={streamType}
              onChange={setStreamType}
              style={{ width: '100%' }}
            >
              <Option value="enhanced">增强节点输出流 (Enhanced)</Option>
              <Option value="basic">基础节点输出流 (Basic)</Option>
              <Option value="snapshots">节点状态快照流 (Snapshots)</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

GraphInner.displayName = 'GraphInner';

const Graph: React.FC<GraphProps> = (props) => {
  return (
    <ReactFlowProvider>
      <GraphInner {...props} />
    </ReactFlowProvider>
  );
};

Graph.displayName = 'Graph';

export default Graph;
