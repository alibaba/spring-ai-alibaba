import React from 'react';
import { Button, Tag, Collapse, Statistic, Row, Col } from 'antd';
import { ClearOutlined } from '@ant-design/icons';
import { useDebugContext } from '../contexts/DebugContext';
import { useChatContext } from '../contexts/ChatContext';
import styles from '../index.module.less';

const { Panel } = Collapse;

const DebugPanel: React.FC = () => {
  const { debugState, clearLogs, addDebugLog } = useDebugContext();
  const { state } = useChatContext();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected':
        return '#52c41a';
      case 'connecting':
        return '#faad14';
      case 'disconnected':
        return '#ff4d4f';
      default:
        return '#d9d9d9';
    }
  };

  const formatLogTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3,
    });
  };

  const getLogColor = (level: string) => {
    switch (level) {
      case 'error':
        return '#ff4d4f';
      case 'warning':
        return '#faad14';
      default:
        return '#1890ff';
    }
  };

  return (
    <div className={styles.debugPanel}>
      <div className={styles.debugHeader}>
        <h4 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>调试面板</h4>
        <Button
          type="text"
          size="small"
          icon={<ClearOutlined />}
          onClick={clearLogs}
          title="清空日志"
        />
      </div>

      <div className={styles.debugContent}>
        {/* Connection Status */}
        <div className={styles.debugSection}>
          <div className={styles.debugSectionTitle}>连接状态</div>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
            <span
              className={`${styles.statusIndicator} ${styles[debugState.connectionStatus.status]}`}
            />
            <span style={{ fontSize: 12, textTransform: 'capitalize' }}>
              {debugState.connectionStatus.status}
            </span>
          </div>
          {debugState.connectionStatus.lastConnected && (
            <div style={{ fontSize: 11, color: '#666' }}>
              最后连接: {debugState.connectionStatus.lastConnected.toLocaleString()}
            </div>
          )}
          {debugState.connectionStatus.errorMessage && (
            <div style={{ fontSize: 11, color: '#ff4d4f', marginTop: 4 }}>
              错误: {debugState.connectionStatus.errorMessage}
            </div>
          )}
        </div>

        {/* Metrics */}
        <div className={styles.debugSection}>
          <div className={styles.debugSectionTitle}>统计信息</div>
          <Row gutter={8}>
            <Col span={12}>
              <Statistic
                title="消息数"
                value={debugState.metrics.messagesCount}
                valueStyle={{ fontSize: 14 }}
              />
            </Col>
            <Col span={12}>
              <Statistic
                title="错误数"
                value={debugState.metrics.errorCount}
                valueStyle={{ fontSize: 14, color: '#ff4d4f' }}
              />
            </Col>
          </Row>
          <Row gutter={8} style={{ marginTop: 8 }}>
            <Col span={12}>
              <Statistic
                title="平均响应"
                value={debugState.metrics.averageResponseTime}
                suffix="ms"
                valueStyle={{ fontSize: 14 }}
              />
            </Col>
            <Col span={12}>
              <div style={{ fontSize: 12, color: '#666' }}>
                最后活动:
                <br />
                {debugState.metrics.lastActivity
                  ? debugState.metrics.lastActivity.toLocaleTimeString()
                  : '无'}
              </div>
            </Col>
          </Row>
        </div>

        {/* Current Session Info */}
        {state.currentSessionId && (
          <div className={styles.debugSection}>
            <div className={styles.debugSectionTitle}>当前会话</div>
            <div className={styles.debugInfo}>
              <div>会话ID: {state.currentSessionId}</div>
              <div>加载状态: {state.isLoading ? '是' : '否'}</div>
              <div>流式传输: {state.isStreaming ? '是' : '否'}</div>
              {state.error && <div>错误: {state.error}</div>}
            </div>
          </div>
        )}

        {/* Debug Logs */}
        <div className={styles.debugSection}>
          <div className={styles.debugSectionTitle}>调试日志</div>
          <Collapse size="small" ghost>
            <Panel header={`日志 (${debugState.debugLogs.length})`} key="1">
              <div style={{ maxHeight: 200, overflowY: 'auto' }}>
                {debugState.debugLogs.length === 0 ? (
                  <div style={{ color: '#999', fontSize: 12, textAlign: 'center', padding: 16 }}>
                    暂无日志
                  </div>
                ) : (
                  debugState.debugLogs.map((log: any, index: number) => (
                    <div
                      key={index}
                      style={{
                        marginBottom: 8,
                        padding: 6,
                        backgroundColor: '#f9f9f9',
                        borderRadius: 4,
                        fontSize: 11,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Tag
                          color={getLogColor(log.level)}
                          style={{ margin: 0, fontSize: 10 }}
                        >
                          {log.level.toUpperCase()}
                        </Tag>
                        <span style={{ color: '#666' }}>
                          {formatLogTime(log.timestamp)}
                        </span>
                      </div>
                      <div style={{ marginTop: 4 }}>{log.message}</div>
                      {log.data && (
                        <pre
                          style={{
                            margin: '4px 0 0 0',
                            fontSize: 10,
                            color: '#666',
                            whiteSpace: 'pre-wrap',
                          }}
                        >
                          {JSON.stringify(log.data, null, 2)}
                        </pre>
                      )}
                    </div>
                  ))
                )}
              </div>
            </Panel>
          </Collapse>
        </div>

        {/* Test Actions */}
        <div className={styles.debugSection}>
          <div className={styles.debugSectionTitle}>测试操作</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <Button
              size="small"
              onClick={() =>
                addDebugLog({
                  level: 'info',
                  message: '测试信息日志',
                  data: { timestamp: new Date().toISOString() },
                })
              }
            >
              添加测试日志
            </Button>
            <Button
              size="small"
              onClick={() =>
                addDebugLog({
                  level: 'warning',
                  message: '测试警告日志',
                })
              }
            >
              添加警告日志
            </Button>
            <Button
              size="small"
              onClick={() =>
                addDebugLog({
                  level: 'error',
                  message: '测试错误日志',
                  data: { error: 'simulated error' },
                })
              }
            >
              添加错误日志
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DebugPanel;
