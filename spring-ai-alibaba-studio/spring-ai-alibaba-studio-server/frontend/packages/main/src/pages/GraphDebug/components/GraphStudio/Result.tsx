import React, { useState, useEffect } from 'react';
import {
  Select,
  Collapse,
  Avatar,
  Typography,
  Space,
  Tag,
  Button,
  Tooltip,
  Divider,
} from 'antd';
import {
  UserOutlined,
  RobotOutlined,
  ClockCircleOutlined,
  DownOutlined,
  RightOutlined,
  EyeOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { GraphStudioEvent } from './index';
import styles from './index.module.less';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;
const { Option } = Select;

interface ResultProps {
  currentThread: string;
  executionResults: any[];
  onThreadSelect: (threadId: string) => void;
  dispatchEvent: (event: GraphStudioEvent) => void;
  selectedNode?: string | null;
}

// 消息类型
interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  nodeId?: string;
  summary?: string;
  details?: any;
}

// Turn数据类型
interface Turn {
  id: string;
  turnNumber: number;
  timestamp: string;
  messages: Message[];
  status: 'running' | 'completed' | 'error';
  totalSteps?: number;
  currentStep?: number;
}

// Result结果展示组件
const Result: React.FC<ResultProps> = ({
  currentThread,
  executionResults,
  onThreadSelect,
  dispatchEvent,
  selectedNode,
}) => {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [activeTurns, setActiveTurns] = useState<string[]>(['1']);
  const [availableThreads, setAvailableThreads] = useState<string[]>([]);

  // 模拟的线程数据
  const mockThreads = [
    'Thread 526b1f3d-6c75-415d-8e6b-e047d8...',
    'Thread 5231f93-d80e-462d-b7ea-8ebe950ca22',
    'Thread 789abc12-3def-4567-8901-234567890abc',
  ];

  // 初始化数据
  useEffect(() => {
    setAvailableThreads(mockThreads);
  }, []);

  // 处理执行结果更新（支持新的流式节点输出）
  useEffect(() => {
    if (executionResults.length === 0) return;
    const latest = executionResults[executionResults.length - 1];

    // execution_start: 开始执行
    if (latest.type === 'execution_start') {
      const newMessage: Message = {
        id: `m_start_${Date.now()}`,
        role: 'system',
        content: `开始执行图工作流: ${latest.data.graphId}`,
        timestamp: latest.timestamp,
        summary: '执行开始',
      };

      setTurns(prev => [{
        id: `turn_${Date.now()}`,
        turnNumber: prev.length + 1,
        timestamp: latest.timestamp,
        status: 'running',
        totalSteps: undefined,
        currentStep: undefined,
        messages: [newMessage],
      }, ...prev]);
      return;
    }

    // node_update: 实时节点更新（支持不同流式类型）
    if (latest.type === 'node_update') {
      const nodeData = latest.data;
      const streamType = latest.streamType || 'enhanced';
      
      // 根据流式类型和数据结构创建不同的消息内容
      let content = '';
      let summary = '';
      let nodeId = '';
      
      switch (streamType) {
        case 'basic':
          // 基础节点输出流 (3.1)
          nodeId = nodeData.node || 'Unknown';
          content = JSON.stringify({
            node: nodeData.node,
            state: nodeData.state,
            subGraph: nodeData.subGraph,
          }, null, 2);
          summary = `📝 ${nodeId} - 基础节点输出`;
          if (nodeData.node === 'END') {
            summary = `🏁 ${nodeId} - 流程结束`;
          }
          break;

        case 'enhanced':
          // 增强节点输出流 (3.3)
          nodeId = nodeData.node_id || 'Unknown';
          if (nodeData.execution_status) {
            switch (nodeData.execution_status) {
              case 'EXECUTING':
                content = `节点 ${nodeData.node_id} 正在执行...`;
                summary = `🔄 ${nodeData.node_id} - 执行中`;
                break;
              case 'SUCCESS':
                content = JSON.stringify({
                  node_id: nodeData.node_id,
                  duration_ms: nodeData.duration_ms,
                  data: nodeData.data,
                  execution_order: nodeData.execution_order,
                }, null, 2);
                summary = `✅ ${nodeData.node_id} - 执行成功 (${nodeData.duration_ms}ms)`;
                break;
              case 'FAILED':
                content = JSON.stringify({
                  node_id: nodeData.node_id,
                  error_message: nodeData.error_message,
                  duration_ms: nodeData.duration_ms,
                }, null, 2);
                summary = `❌ ${nodeData.node_id} - 执行失败`;
                break;
              case 'SKIPPED':
                content = `节点 ${nodeData.node_id} 被跳过`;
                summary = `⏭️ ${nodeData.node_id} - 跳过`;
                break;
              default:
                content = JSON.stringify(nodeData, null, 2);
                summary = `${nodeData.node_id} - ${nodeData.execution_status}`;
            }
          } else {
            content = JSON.stringify(nodeData, null, 2);
            summary = `节点更新 - ${nodeData.node_id || 'Unknown'}`;
          }
          break;

        default:
          nodeId = nodeData.node_id || nodeData.node || 'Unknown';
          content = JSON.stringify(nodeData, null, 2);
          summary = `节点更新 - ${nodeId}`;
      }

      const newMessage: Message = {
        id: `m_${Date.now()}_${nodeId || Math.random()}`,
        role: 'assistant',
        content,
        timestamp: latest.timestamp,
        nodeId: nodeId,
        summary,
        details: nodeData,
      };

      setTurns(prev => {
        const updated = [...prev];
        if (updated.length > 0) {
          // 如果是同一个节点的更新，根据流式类型处理
          const lastMessage = updated[0].messages[updated[0].messages.length - 1];
          if (streamType === 'enhanced' && lastMessage && lastMessage.nodeId === nodeId && nodeData.execution_status === 'EXECUTING') {
            // 如果是执行中状态，替换之前的执行中消息
            const existingIndex = updated[0].messages.findIndex(m => 
              m.nodeId === nodeId && m.summary?.includes('执行中')
            );
            if (existingIndex >= 0) {
              updated[0].messages[existingIndex] = newMessage;
            } else {
              updated[0].messages.push(newMessage);
            }
          } else {
            updated[0].messages.push(newMessage);
          }
          
          // 更新当前步骤信息
          if (nodeData.execution_order) {
            updated[0].currentStep = nodeData.execution_order;
          }
        }
        return updated;
      });
      return;
    }

    // state_update: 状态快照流更新 (3.2)
    if (latest.type === 'state_update') {
      const stateData = latest.data;
      const streamType = latest.streamType || 'snapshots';
      
      const content = JSON.stringify(stateData, null, 2);
      const summary = `📊 状态快照更新 - ${Object.keys(stateData).join(', ')}`;
      
      const newMessage: Message = {
        id: `m_state_${Date.now()}`,
        role: 'assistant',
        content,
        timestamp: latest.timestamp,
        summary,
        details: stateData,
      };

      setTurns(prev => {
        const updated = [...prev];
        if (updated.length > 0) {
          updated[0].messages.push(newMessage);
        }
        return updated;
      });
      return;
    }

    // execution_complete: 执行完成
    if (latest.type === 'execution_complete') {
      const newMessage: Message = {
        id: `m_complete_${Date.now()}`,
        role: 'system',
        content: `图工作流执行完成: ${latest.data.graphId}`,
        timestamp: latest.timestamp,
        summary: '✅ 执行完成',
      };

      setTurns(prev => {
        const updated = [...prev];
        if (updated.length > 0) {
          updated[0].messages.push(newMessage);
          updated[0].status = 'completed';
        }
        return updated;
      });
      return;
    }

    // 兼容旧格式：node-step
    if (latest.type === 'node-step') {
      const newMessage: Message = {
        id: `m_${Date.now()}_${latest.nodeId}`,
        role: 'assistant',
        content: JSON.stringify(latest.data, null, 2),
        timestamp: latest.timestamp,
        nodeId: latest.nodeId,
        summary: latest.summary || `节点 ${latest.nodeId} 执行`,
      };

      setTurns(prev => {
        const updated = [...prev];
        if (updated.length === 0) {
          updated.push({
            id: '1',
            turnNumber: 1,
            timestamp: latest.timestamp,
            status: 'running',
            totalSteps: undefined,
            currentStep: undefined,
            messages: [newMessage],
          });
          return updated;
        }
        // 追加到第一个 Turn
        updated[0].messages.push(newMessage);
        updated[0].status = 'running';
        return updated;
      });
      return;
    }

    // 其他类型的兼容处理
    const newMessage: Message = {
      id: `m_${Date.now()}`,
      role: 'assistant',
      content: JSON.stringify(latest.data, null, 2),
      timestamp: latest.timestamp,
      summary: `执行结果 - ${latest.type}`,
    };
    
    setTurns(prevTurns => {
      const updatedTurns = [...prevTurns];
      if (updatedTurns.length > 0) {
        updatedTurns[0].messages.push(newMessage);
      } else {
        updatedTurns.push({
          id: '1',
          turnNumber: 1,
          timestamp: latest.timestamp,
          status: 'completed',
          messages: [newMessage],
        });
      }
      return updatedTurns;
    });
  }, [executionResults]);

  // 渲染消息头像
  const renderMessageAvatar = (message: Message) => {
    const avatarProps = {
      size: 32 as const,
      style: { flexShrink: 0 },
    };

    switch (message.role) {
      case 'user':
        return <Avatar {...avatarProps} icon={<UserOutlined />} />;
      case 'assistant':
        return <Avatar {...avatarProps} icon={<RobotOutlined />} style={{ backgroundColor: '#52c41a' }} />;
      case 'system':
        return <Avatar {...avatarProps} style={{ backgroundColor: '#1890ff' }}>S</Avatar>;
      default:
        return <Avatar {...avatarProps} icon={<RobotOutlined />} />;
    }
  };

  // 处理节点点击，高亮左侧对应节点
  const handleNodeClick = (nodeId: string) => {
    dispatchEvent({
      type: 'node-selected',
      payload: { nodeId },
    });
  };

  // 渲染消息内容
  const renderMessageContent = (message: Message) => {
    const isHighlighted = selectedNode === message.nodeId;
    
    return (
      <div className={`${styles['message-content']} ${isHighlighted ? styles['message-highlighted'] : ''}`}>
        <div className={styles['message-time']}>
          <Space size={4}>
            <ClockCircleOutlined />
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {message.timestamp}
            </Text>
            {message.nodeId && (
              <Tag 
                color={isHighlighted ? "orange" : "blue"}
                style={{ cursor: 'pointer' }}
                onClick={() => handleNodeClick(message.nodeId!)}
              >
                {message.nodeId}
              </Tag>
            )}
          </Space>
        </div>

        {message.summary && (
          <Text strong style={{ display: 'block', marginBottom: 4 }}>
            {message.summary}
          </Text>
        )}

        <div className={styles['message-text']}>
          <Paragraph
            copyable={(message.content ?? '').length > 50}
            ellipsis={{ rows: 3, expandable: true, symbol: '展开' }}
          >
            {message.content || ''}
          </Paragraph>
        </div>
      </div>
    );
  };

  // 渲染Turn状态
  const renderTurnStatus = (turn: Turn) => {
    const statusConfig = {
      running: { color: '#1890ff', text: '运行中' },
      completed: { color: '#52c41a', text: '已完成' },
      error: { color: '#ff4d4f', text: '错误' },
    };

    const config = statusConfig[turn.status];

    return (
      <Space>
        <Tag color={config.color}>{config.text}</Tag>
        {turn.totalSteps && (
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {turn.currentStep || 0}/{turn.totalSteps} 步骤
          </Text>
        )}
      </Space>
    );
  };

  // 处理Turn展开/收起
  const handleTurnChange = (keys: string | string[]) => {
    setActiveTurns(Array.isArray(keys) ? keys : [keys]);
  };

  // 处理查看详细状态
  const handleViewState = (turnId: string) => {
    dispatchEvent({
      type: 'state-updated',
      payload: { action: 'view-state', turnId },
    });
  };

  return (
    <div className={styles['result-area']}>
      {/* 顶部线程选择 */}
      <div className={styles['result-header']}>
        <Select
          style={{ width: '100%' }}
          placeholder="选择线程"
          value={currentThread || undefined}
          onChange={onThreadSelect}
          dropdownMatchSelectWidth={false}
        >
          {availableThreads.map((thread, index) => (
            <Option key={thread} value={thread}>
              Thread {thread.substring(0, 20)}...
            </Option>
          ))}
        </Select>
      </div>

      {/* Turn列表 */}
      <div className={styles['result-content']}>
        <Collapse
          activeKey={activeTurns}
          onChange={handleTurnChange}
          expandIcon={({ isActive }) => (
            isActive ? <DownOutlined /> : <RightOutlined />
          )}
        >
          {turns.map(turn => (
            <Panel
              key={turn.id}
              header={
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                  <Space>
                    <Text strong>TURN {turn.turnNumber}</Text>
                    {renderTurnStatus(turn)}
                  </Space>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {turn.timestamp}
                  </Text>
                </div>
              }
              extra={
                <Space onClick={e => e.stopPropagation()}>
                  <Tooltip title="查看状态">
                    <Button
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewState(turn.id)}
                    />
                  </Tooltip>
                  <Button size="small" icon={<MoreOutlined />} />
                </Space>
              }
              className={styles['turn-card']}
            >
              <div className={styles['turn-content']}>
                {turn.messages.map(message => (
                  <div key={message.id} className={styles['message-item']}>
                    {renderMessageAvatar(message)}
                    {renderMessageContent(message)}
                  </div>
                ))}

                {turn.status === 'running' && (
                  <div style={{ textAlign: 'center', padding: '16px' }}>
                    <Text type="secondary">执行中...</Text>
                  </div>
                )}
              </div>
            </Panel>
          ))}
        </Collapse>

        {turns.length === 0 && (
          <div style={{
            textAlign: 'center',
            padding: '40px 20px',
            color: '#999',
          }}
          >
            <Text>暂无执行结果</Text>
          </div>
        )}
      </div>
    </div>
  );
};

export default Result;
