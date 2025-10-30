export interface IGraphData {
  id: string;
  name: string;
  description?: string;
  nodes?: GraphNode[];
  edges?: GraphEdge[];
  
  //后端observability字段
  title?: string;
  tags?: string[];
  mermaidGraph?: string;
  stateGraph?: any;
}

export interface GraphNode {
  id: string;
  type: string;
  data: Record<string, any>;
  position: {
    x: number;
    y: number;
  };
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;
  targetHandle?: string;
  label?: string;
}

export interface IGraphCard {
  id: string;
  name: string;
  description?: string;
  tags?: string[];
  gmt_modified: string;
  status: 'ACTIVE' | 'DRAFT' | 'DISABLED';
}

export interface GraphRunActionParam {
  graphName: string;
  input: Record<string, any>;
  stream?: boolean;
  // Add other relevant fields as needed
}

export interface GraphRunResult {
  id: string;
  status: string;
  result: any;
  telemetry?: any;
  // Add other relevant fields as needed
}

export interface GraphOptions {
  // Graph-specific configuration options
  maxExecutionTime?: number;
  enableParallelExecution?: boolean;
  debugMode?: boolean;
  // Add other configuration options as needed
}

export interface GraphStudioConfig {
  graphOptions: GraphOptions;
  // Add other studio-specific configurations
}

// 基础节点输出接口
export interface NodeOutput {
  node: string;                 // 节点标识符
  state: OverAllState;          // 与节点关联的状态
  subGraph: boolean;            // 是否为子图
}

// 状态快照接口
export interface StateSnapshot {
  [key: string]: any;          // 动态键值对，包含节点状态数据
}

// 增强节点输出接口 
export interface EnhancedNodeOutput {
  node_id: string;              // 节点ID
  execution_status: 'EXECUTING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'; // 执行状态
  start_time?: string;          // 开始执行时间 (ISO 8601)
  end_time?: string;            // 完成时间 (ISO 8601)
  duration_ms?: number;         // 执行时间(毫秒)
  data?: Record<string, any>;   // 节点业务数据输出
  error_message?: string;       // 错误信息(如果执行失败)
  input_data?: Record<string, any>; // 输入数据(可选，用于调试)
  execution_order?: number;     // 执行序号(整个流程中的执行顺序)
  is_final?: boolean;          // 是否为最终节点
  parent_nodes?: string[];     // 父节点ID列表(依赖的上游节点)
  timestamp?: string;          // 时间戳(兼容旧版本)
}

// 整体状态接口
export interface OverAllState {
  data?: Record<string, any>;
  [key: string]: any;
}
