
import { request } from '@/request';
import { IGraphData, IGraphCard, EnhancedNodeOutput, NodeOutput, StateSnapshot } from '@/types/graph';

const OBSERVABILITY_BASE_URL = 'http://localhost:8091/observability/v1';//后端需要进行跨域配置

/**
 * GraphDebug服务类
 */
class GraphDebugService {

  private normalizeStatus(status?: string): IGraphCard['status'] {
    const s = (status || '').toUpperCase();
    if (s === 'ACTIVE') return 'ACTIVE';
    if (s === 'DRAFT') return 'DRAFT';
    if (s === 'DISABLED' || s === 'INACTIVE') return 'DISABLED';
    return 'ACTIVE';
  }

  private mapExecutionStatus(status?: string): EnhancedNodeOutput['execution_status'] {
    const s = (status || '').toUpperCase();
    if (s === 'RUNNING' || s === 'EXECUTING') return 'EXECUTING';
    if (s === 'SUCCESS' || s === 'COMPLETED') return 'SUCCESS';
    if (s === 'ERROR' || s === 'FAILED') return 'FAILED';
    if (s === 'SKIPPED') return 'SKIPPED';
    return 'EXECUTING';
  }

  /**
   * 从 Mermaid 图中提取节点和边信息
   */
  private extractNodesFromMermaid(mermaidGraph?: string): { nodes: any[]; edges: any[] } {
    const nodes: any[] = [];
    const edges: any[] = [];

    if (!mermaidGraph) {
      return { nodes, edges };
    }

    try {
      const lines = mermaidGraph.split('\n');
      let yPosition = 100;
      const nodePositions: { [key: string]: { x: number; y: number } } = {};
      
      lines.forEach((line, index) => {
        const trimmedLine = line.trim();
        
        // 提取节点定义 (例如: summarizer("summarizer"))
        const nodeMatch = trimmedLine.match(/^([^()\s]+)\(([^)]*)\)/);
        if (nodeMatch) {
          const nodeId = nodeMatch[1];
          const nodeLabel = nodeMatch[2].replace(/['"]/g, ''); // 移除引号
          
          // 跳过特殊节点定义行
          if (!nodeId.startsWith('__') || nodeId === '__START__' || nodeId === '__END__') {
            const position = { x: 200 + (nodes.length % 3) * 300, y: yPosition + Math.floor(nodes.length / 3) * 150 };
            nodePositions[nodeId] = position;
            
            nodes.push({
              id: nodeId,
              type: nodeId.startsWith('__') ? 'special' : 'default',
              data: {
                label: nodeLabel || nodeId,
                type: nodeId.startsWith('__') ? 'special' : 'default',
              },
              position,
            });
          }
        }
        
        // 提取边连接 (例如: __START__:::__START__ --> summarizer:::summarizer)
        const edgeMatch = trimmedLine.match(/^([^:]+)(?:::.*?)?\s*-->\s*([^:]+)(?:::.*?)?$/);
        if (edgeMatch) {
          const sourceId = edgeMatch[1].trim();
          const targetId = edgeMatch[2].trim();
          
          edges.push({
            id: `${sourceId}-${targetId}`,
            source: sourceId,
            target: targetId,
          });
        }
        
        // 提取条件边 (例如: feedback_classifier:::feedback_classifier -.->|negative| summarizer:::summarizer)
        const conditionalEdgeMatch = trimmedLine.match(/^([^:]+)(?:::.*?)?\s*\.->(?:\|([^|]+)\|)?\s*([^:]+)(?:::.*?)?$/);
        if (conditionalEdgeMatch) {
          const sourceId = conditionalEdgeMatch[1].trim();
          const label = conditionalEdgeMatch[2]?.trim();
          const targetId = conditionalEdgeMatch[3].trim();
          
          edges.push({
            id: `${sourceId}-${targetId}-${label || 'conditional'}`,
            source: sourceId,
            target: targetId,
            label: label,
            type: 'conditional',
          });
        }
      });
      
      
    } catch (error) {
    }

    return { nodes, edges };
  }

  /**
   * 从 StateGraph 中提取节点和边信息
   */
  private extractNodesAndEdges(stateGraph: any, mermaidGraph?: string): { nodes: any[]; edges: any[] } {
    // 优先从 mermaidGraph 中提取节点和边信息
    if (mermaidGraph) {
      const result = this.extractNodesFromMermaid(mermaidGraph);
      if (result.nodes.length > 0) {
        return result;
      }
    }

    const nodes: any[] = [];
    const edges: any[] = [];

    if (!stateGraph) {
      return { nodes, edges };
    }

    // 处理节点
    if (stateGraph.nodes) {
      const nodeMap = stateGraph.nodes;
      Object.keys(nodeMap).forEach((nodeId, index) => {
        const nodeInfo = nodeMap[nodeId];
        nodes.push({
          id: nodeId,
          type: nodeInfo?.type || 'node',
          data: {
            label: nodeInfo?.label || nodeId,
            type: nodeInfo?.type || 'node',
            ...nodeInfo
          },
          position: nodeInfo?.position || { x: 100 + index * 200, y: 200 },
        });
      });
    }

    // 处理边
    if (stateGraph.edges) {
      const edgeMap = stateGraph.edges;
      Object.keys(edgeMap).forEach((edgeId) => {
        const edgeInfo = edgeMap[edgeId];
        if (edgeInfo?.source && edgeInfo?.target) {
          edges.push({
            id: edgeId,
            source: edgeInfo.source,
            target: edgeInfo.target,
            label: edgeInfo.label,
          });
        }
      });
    }

    return { nodes, edges };
  }

  /**
   * 获取图列表
   * @param params.current 当前页码
   * @param params.size 每页大小
   * @param params.ownerID 用户唯一标识符（必需）
   */
  async getGraphList(params: { current: number; size: number; ownerID?: string }): Promise<{
    records: IGraphCard[];
    total: number;
  }> {
    const { ownerID = 'saa' } = params;
    
    const response = await request({
      url: `${OBSERVABILITY_BASE_URL}/flows`,
      method: 'GET',
      params: { ownerID },
    });
    
    // 根据实际API响应格式，直接处理数组数据
    let apiRecords: any[] = [];
    
    if (Array.isArray(response.data)) {
      // API直接返回数组格式
      apiRecords = response.data;
    } else if (response.data && Array.isArray(response.data.records)) {
      // 兼容分页格式 { records: [...], total: 10 }
      apiRecords = response.data.records;
    } else if (response.data && Array.isArray(response.data.flows)) {
      // 兼容包装格式 { flows: [...] }
      apiRecords = response.data.flows;
    } else {
      apiRecords = [];
    }

    // 映射到前端数据格式
    const records: IGraphCard[] = apiRecords.map((flow: any) => {
      return {
        id: flow.id || '',
        name: flow.title || flow.name || '未命名流程', // API使用title字段
        description: flow.description || '',
        tags: Array.isArray(flow.tags) ? flow.tags : [],
        gmt_modified: flow.updatedAt || flow.gmt_modified || flow.createTime || new Date().toISOString(),
        status: this.normalizeStatus(flow.status || 'ACTIVE'), // 默认为ACTIVE状态
      };
    });

    return {
      records,
      total: records.length,
    };
  }

  /**
   * 根据ID获取图详情
   */
  async getGraphById(graphId: string): Promise<IGraphData> {
    const response = await request({
      url: `${OBSERVABILITY_BASE_URL}/flows/${graphId}`,
      method: 'GET',
    });
    
    const flow: any = response.data;

    // 从 stateGraph 和 mermaidGraph 中提取节点和边信息
    const { nodes, edges } = this.extractNodesAndEdges(flow.stateGraph, flow.mermaidGraph);


    return {
      id: flow.id || graphId,
      name: flow.title || flow.name || '未命名流程', // API使用title字段
      description: flow.description || '',
      title: flow.title || flow.name || '未命名流程', // API使用title字段
      tags: Array.isArray(flow.tags) ? flow.tags : [],
      nodes,
      edges,
      stateGraph: flow.stateGraph,
      mermaidGraph: flow.mermaidGraph,
    };
  }

  /**
   * 执行图并获取增强节点输出流
   */
  async executeGraphEnhanced(
    graphId: string,
    inputText: string,
    onNodeUpdate: (output: EnhancedNodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      // 设置当前图
      await this.setCurrentGraph(graphId);


      // 创建增强节点输出流
      const url = new URL(`${OBSERVABILITY_BASE_URL}/graph/node/stream_enhanced`, window.location.origin);
      url.searchParams.append('text', inputText);
      
      
      const es = new EventSource(url.toString());
      let nodeCount = 0;
      let isCompleted = false;

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          nodeCount++;
          
          
          onNodeUpdate(data);
          
          // 检查是否为最终节点
          if (data?.is_final || data?.execution_status === 'COMPLETED') {
            isCompleted = true;
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (error: Event) => {
        if (onError) onError(error);
      };

      const handleOpen = () => {
      };

      es.addEventListener('open', handleOpen);
      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        isCompleted = true;
        es.removeEventListener('open', handleOpen);
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 执行图并获取基础节点输出流
   */
  async executeGraphBasic(
    graphId: string,
    inputText: string,
    onNodeUpdate: (output: NodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      // 设置当前图
      await this.setCurrentGraph(graphId);


      // 创建基础节点输出流
      const url = new URL(`${OBSERVABILITY_BASE_URL}/graph/node/stream`, window.location.origin);
      url.searchParams.append('text', inputText);
      const es = new EventSource(url.toString());

      let isCompleted = false;

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          onNodeUpdate(data);
          
          if (data?.node === 'END') {
            isCompleted = true;
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (event: Event) => {
        if (onError) onError(event);
      };

      const handleOpen = () => {
      };

      es.addEventListener('open', handleOpen);
      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        isCompleted = true;
        es.removeEventListener('open', handleOpen);
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 执行图并获取节点状态快照流
   */
  async executeGraphSnapshots(
    graphId: string,
    inputText: string,
    onStateUpdate: (snapshot: StateSnapshot) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      // 设置当前图
      await this.setCurrentGraph(graphId);

      // 创建节点状态快照流
      const url = new URL(`${OBSERVABILITY_BASE_URL}/graph/node/stream_snapshots`, window.location.origin);
      url.searchParams.append('text', inputText);
      const es = new EventSource(url.toString());

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          onStateUpdate(data);
          // 检查是否包含完成标识
          if (data?.final_result !== undefined) {
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (event: Event) => {
        if (onError) onError(event);
      };

      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 独立节点流接口 - 增强节点输出流
   */
  async executeNodeEnhanced(
    inputText: string,
    onNodeUpdate: (output: EnhancedNodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      const url = new URL(`${OBSERVABILITY_BASE_URL}/node/stream_enhanced`, window.location.origin);
      url.searchParams.append('text', inputText);
      const es = new EventSource(url.toString());

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          onNodeUpdate(data);
          if (data?.is_final && data?.execution_status === 'SUCCESS') {
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (event: Event) => {
        if (onError) onError(event);
      };

      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 独立节点流接口 - 基础节点输出流
   */
  async executeNodeBasic(
    inputText: string,
    onNodeUpdate: (output: NodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      const url = new URL(`${OBSERVABILITY_BASE_URL}/node/stream`, window.location.origin);
      url.searchParams.append('text', inputText);
      const es = new EventSource(url.toString());

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          onNodeUpdate(data);
          if (data?.node === 'END') {
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (event: Event) => {
        if (onError) onError(event);
      };

      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 独立节点流接口 - 节点状态快照流 
   */
  async executeNodeSnapshots(
    inputText: string,
    onStateUpdate: (snapshot: StateSnapshot) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    try {
      const url = new URL(`${OBSERVABILITY_BASE_URL}/node/stream_snapshots`, window.location.origin);
      url.searchParams.append('text', inputText);
      const es = new EventSource(url.toString());

      const handleMessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          onStateUpdate(data);
          // 检查是否包含完成标识
          if (data?.final_result !== undefined) {
            if (onComplete) onComplete();
          }
        } catch (err) {
          if (onError) onError(err);
        }
      };

      const handleError = (event: Event) => {
        if (onError) onError(event);
      };

      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      return () => {
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      return () => {};
    }
  }

  /**
   * 向后兼容的执行图方法（默认使用增强节点输出流）
   */
  async executeGraph(
    graphId: string,
    inputText: string,
    onNodeUpdate: (output: EnhancedNodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    return this.executeGraphEnhanced(graphId, inputText, onNodeUpdate, onError, onComplete);
  }


  /**
   * 设置当前活跃图
   */
  async setCurrentGraph(graphId: string): Promise<void> {
    await request({
      url: `${OBSERVABILITY_BASE_URL}/graph/setCurrentGraph`,
      method: 'POST',
      params: { graphId },
    });
  }

  /**
   * 获取当前活跃图
   */
  async getCurrentGraph(): Promise<IGraphData | null> {
    const response = await request({
      url: `${OBSERVABILITY_BASE_URL}/graph/getCurrentGraph`,
      method: 'GET',
    });
    
    const graphFlow: any = response.data;

    if (!graphFlow) {
      return null;
    }

    // 从 stateGraph 和 mermaidGraph 中提取节点和边信息
    const { nodes, edges } = this.extractNodesAndEdges(graphFlow.stateGraph, graphFlow.mermaidGraph);

    return {
      id: graphFlow.graphId,
      name: graphFlow.title || graphFlow.name || '未命名流程',
      description: graphFlow.description || '',
      title: graphFlow.title || graphFlow.name || '未命名流程',
      tags: graphFlow.tags || [],
      nodes,
      edges,
      stateGraph: graphFlow.stateGraph,
      mermaidGraph: graphFlow.mermaidGraph,
    };
  }
}


const graphDebugService = new GraphDebugService();
export default graphDebugService;