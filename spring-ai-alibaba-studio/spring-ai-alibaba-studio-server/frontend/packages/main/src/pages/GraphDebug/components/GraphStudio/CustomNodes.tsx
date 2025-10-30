import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import styles from './CustomNodes.module.less';

// 节点执行状态类型
export type NodeExecutionStatus = 'EXECUTING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | null;

// 根据执行状态获取节点样式
const getNodeStyle = (executionStatus: NodeExecutionStatus, selected: boolean, defaultBg: string, defaultBorder: string) => {
  if (executionStatus === 'EXECUTING') {
    return {
      backgroundColor: '#e6f7ff',
      borderColor: '#1890ff',
      borderWidth: '3px',
      boxShadow: '0 0 10px rgba(24, 144, 255, 0.5)',
    };
  }
  if (executionStatus === 'SUCCESS') {
    return {
      backgroundColor: '#f6ffed',
      borderColor: '#52c41a',
      borderWidth: '2px',
    };
  }
  if (executionStatus === 'FAILED') {
    return {
      backgroundColor: '#fff2e8',
      borderColor: '#ff4d4f',
      borderWidth: '2px',
    };
  }
  if (executionStatus === 'SKIPPED') {
    return {
      backgroundColor: '#fafafa',
      borderColor: '#d9d9d9',
      borderWidth: '2px',
      opacity: 0.6,
    };
  }
  if (selected) {
    return {
      backgroundColor: '#fff7e6',
      borderColor: '#fa8c16',
      borderWidth: '2px',
    };
  }
  return {
    backgroundColor: defaultBg,
    borderColor: defaultBorder,
    borderWidth: '2px',
  };
};

// 标准节点样式
export const StandardNode = memo(({ data, selected }: NodeProps) => {
  const executionStatus = data.executionStatus as NodeExecutionStatus;
  const style = getNodeStyle(executionStatus, selected, '#f5f7fa', '#d9d9d9');
  
  return (
    <div 
      className={`${styles['standard-node']} ${selected ? styles.selected : ''} ${executionStatus === 'EXECUTING' ? styles.executing : ''}`}
      style={style}
    >
      <Handle type="target" position={Position.Left} className={styles.handle} />
      <div className={styles['node-label']}>
        {data.label || data.name || data.id}
        {executionStatus === 'EXECUTING' && ' ⏳'}
        {executionStatus === 'SUCCESS' && ' ✅'}
        {executionStatus === 'FAILED' && ' ❌'}
        {executionStatus === 'SKIPPED' && ' ⏭️'}
      </div>
      <Handle type="source" position={Position.Right} className={styles.handle} />
    </div>
  );
});

StandardNode.displayName = 'StandardNode';

// START节点（圆形，绿色）
export const StartNode = memo(({ data, selected }: NodeProps) => {
  const executionStatus = data.executionStatus as NodeExecutionStatus;
  let style: any = {
    backgroundColor: '#52c41a',
    borderColor: '#389e0d',
    borderWidth: '2px',
  };
  
  if (executionStatus === 'EXECUTING') {
    style = {
      backgroundColor: '#52c41a',
      borderColor: '#1890ff',
      borderWidth: '3px',
      boxShadow: '0 0 10px rgba(24, 144, 255, 0.5)',
    };
  } else if (executionStatus === 'SUCCESS') {
    style = {
      backgroundColor: '#52c41a',
      borderColor: '#237804',
      borderWidth: '2px',
    };
  } else if (selected) {
    style.borderColor = '#fa8c16';
  }
  
  return (
    <div 
      className={`${styles['circle-node']} ${styles['start-node']} ${selected ? styles.selected : ''} ${executionStatus === 'EXECUTING' ? styles.executing : ''}`}
      style={style}
    >
      <div className={styles['node-label']}>
        {data.label || 'START'}
        {executionStatus === 'EXECUTING' && ' ⏳'}
        {executionStatus === 'SUCCESS' && ' ✅'}
      </div>
      <Handle type="source" position={Position.Right} className={styles.handle} />
    </div>
  );
});

StartNode.displayName = 'StartNode';

// END节点（圆形，红色）
export const EndNode = memo(({ data, selected }: NodeProps) => {
  const executionStatus = data.executionStatus as NodeExecutionStatus;
  let style: any = {
    backgroundColor: '#ff4d4f',
    borderColor: '#cf1322',
    borderWidth: '2px',
  };
  
  if (executionStatus === 'EXECUTING') {
    style = {
      backgroundColor: '#ff4d4f',
      borderColor: '#1890ff',
      borderWidth: '3px',
      boxShadow: '0 0 10px rgba(24, 144, 255, 0.5)',
    };
  } else if (executionStatus === 'SUCCESS') {
    style = {
      backgroundColor: '#ff4d4f',
      borderColor: '#a8071a',
      borderWidth: '2px',
    };
  } else if (selected) {
    style.borderColor = '#fa8c16';
  }
  
  return (
    <div 
      className={`${styles['circle-node']} ${styles['end-node']} ${selected ? styles.selected : ''} ${executionStatus === 'EXECUTING' ? styles.executing : ''}`}
      style={style}
    >
      <Handle type="target" position={Position.Left} className={styles.handle} />
      <div className={styles['node-label']}>
        {data.label || 'END'}
        {executionStatus === 'EXECUTING' && ' ⏳'}
        {executionStatus === 'SUCCESS' && ' ✅'}
      </div>
    </div>
  );
});

EndNode.displayName = 'EndNode';

// AI处理节点
export const AINode = memo(({ data, selected }: NodeProps) => {
  const executionStatus = data.executionStatus as NodeExecutionStatus;
  const style = getNodeStyle(executionStatus, selected, '#f0f5ff', '#597ef7');
  
  return (
    <div 
      className={`${styles['ai-node']} ${selected ? styles.selected : ''} ${executionStatus === 'EXECUTING' ? styles.executing : ''}`}
      style={style}
    >
      <Handle type="target" position={Position.Left} className={styles.handle} />
      <div className={styles['node-icon']}>🤖</div>
      <div className={styles['node-label']}>
        {data.label || data.name || data.id}
        {executionStatus === 'EXECUTING' && ' ⏳'}
        {executionStatus === 'SUCCESS' && ' ✅'}
        {executionStatus === 'FAILED' && ' ❌'}
        {executionStatus === 'SKIPPED' && ' ⏭️'}
      </div>
      <Handle type="source" position={Position.Right} className={styles.handle} />
    </div>
  );
});

AINode.displayName = 'AINode';

// 处理器节点
export const ProcessorNode = memo(({ data, selected }: NodeProps) => {
  const executionStatus = data.executionStatus as NodeExecutionStatus;
  const style = getNodeStyle(executionStatus, selected, '#fff7e6', '#ffa940');
  
  return (
    <div 
      className={`${styles['processor-node']} ${selected ? styles.selected : ''} ${executionStatus === 'EXECUTING' ? styles.executing : ''}`}
      style={style}
    >
      <Handle type="target" position={Position.Left} className={styles.handle} />
      <div className={styles['node-icon']}>⚙️</div>
      <div className={styles['node-label']}>
        {data.label || data.name || data.id}
        {executionStatus === 'EXECUTING' && ' ⏳'}
        {executionStatus === 'SUCCESS' && ' ✅'}
        {executionStatus === 'FAILED' && ' ❌'}
        {executionStatus === 'SKIPPED' && ' ⏭️'}
      </div>
      <Handle type="source" position={Position.Right} className={styles.handle} />
    </div>
  );
});

ProcessorNode.displayName = 'ProcessorNode';

// 节点类型映射
export const nodeTypes = {
  standard: StandardNode,
  start: StartNode,
  end: EndNode,
  ai: AINode,
  processor: ProcessorNode,
};

