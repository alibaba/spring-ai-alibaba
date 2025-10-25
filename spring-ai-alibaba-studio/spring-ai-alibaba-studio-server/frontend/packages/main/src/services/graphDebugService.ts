
import { request } from '@/request';
import { 
  mockGraphData, 
  mockExecutionSteps, 
  mockGraphList,
  mockFeedbackAnalysisGraph,
  mockFeedbackAnalysisSteps,
  mockDocumentPipelineGraph,
  mockDocumentPipelineSteps
} from '@/mock/graphmock';
import { IGraphData, IGraphCard, EnhancedNodeOutput } from '@/types/graph';

const OBSERVABILITY_BASE_URL = '/observability/v1';

/**
 * GraphDebug服务类
 */
class GraphDebugService {
  /**
   * 根据图ID获取对应的mock数据
   */
  private getMockDataByGraphId(graphId: string) {
    switch (graphId) {
      case 'demo-graph-1':
        return {
          graphData: mockFeedbackAnalysisGraph,
          executionSteps: mockFeedbackAnalysisSteps,
        };
      case 'demo-graph-2':
        return {
          graphData: mockDocumentPipelineGraph,
          executionSteps: mockDocumentPipelineSteps,
        };
      default:
        // 默认返回客户反馈分析
        return {
          graphData: mockFeedbackAnalysisGraph,
          executionSteps: mockFeedbackAnalysisSteps,
        };
    }
  }

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
   * 获取图列表
   */
  async getGraphList(params: { current: number; size: number }): Promise<{
    records: IGraphCard[];
    total: number;
  }> {
    try {
      const response = await request({
        url: `${OBSERVABILITY_BASE_URL}/flows`,
        method: 'GET',
        // 可按需传递分页或owner参数；当前与后端约定可能不同，这里先不传
      });
      const apiRecords: any[] = response.data;

      const records: IGraphCard[] = (apiRecords || []).map((g: any) => ({
        id: g.id || g.flowId || g.graphId,
        name: g.name || g.flowName || g.title || '未命名流程',
        description: g.description || g.remark || '',
        tags: g.tags || [],
        gmt_modified: g.gmt_modified || g.gmtModified || g.updatedAt || new Date().toISOString(),
        status: this.normalizeStatus(g.status as string),
      }));

      return {
        records,
        total: Array.isArray(apiRecords) ? apiRecords.length : records.length,
      };
    } catch (e) {
      const records: IGraphCard[] = mockGraphList.map((g) => ({
        id: g.id,
        name: g.name,
        description: g.description,
        tags: g.tags,
        gmt_modified: g.gmt_modified,
        status: this.normalizeStatus(g.status as string),
      }));
      return {
        records,
        total: records.length,
      };
    }
  }

  /**
   * 根据ID获取图详情
   */
  async getGraphById(graphId: string): Promise<IGraphData> {
    try {
      const response = await request({
        url: `${OBSERVABILITY_BASE_URL}/flows/${graphId}`,
        method: 'GET',
      });
      const data: any = response.data;

      const nodes = (data?.nodes || []).map((n: any) => ({
        id: n.id,
        type: n.data?.type || n.type || 'node',
        data: n.data || {},
        position: n.position || { x: 100, y: 100 },
      }));

      const edges = (data?.edges || []).map((e: any) => ({
        id: e.id || `${e.source}-${e.target}`,
        source: e.source,
        target: e.target,
        label: e.label,
      }));

      // 若后端暂不返回节点/边，则回退mock结构
      if (!nodes.length && !edges.length) {
        const mockData = this.getMockDataByGraphId(graphId);
        const mnodes = (mockData.graphData.nodes || []).map((n: any) => ({
          id: n.id,
          type: n.data?.type || 'node',
          data: n.data || {},
          position: n.position || { x: 100, y: 100 },
        }));
        const medges = (mockData.graphData.edges || []).map((e: any) => ({
          id: e.id || `${e.source}-${e.target}`,
          source: e.source,
          target: e.target,
          label: e.label,
        }));
        return {
          id: graphId,
          name: data?.name || data?.title || mockData.graphData.name,
          description: data?.description || mockData.graphData.description,
          title: data?.name || data?.title || mockData.graphData.name,
          tags: data?.tags || ['demo'],
          nodes: mnodes,
          edges: medges,
        };
      }

      return {
        id: graphId,
        name: data?.name || data?.title || '未命名流程',
        description: data?.description || '',
        title: data?.name || data?.title || '未命名流程',
        tags: data?.tags || [],
        nodes,
        edges,
      };
    } catch (e) {
      const mockData = this.getMockDataByGraphId(graphId);
      const nodes = (mockData.graphData.nodes || []).map((n: any) => ({
        id: n.id,
        type: n.data?.type || 'node',
        data: n.data || {},
        position: n.position || { x: 100, y: 100 },
      }));
      const edges = (mockData.graphData.edges || []).map((e: any) => ({
        id: e.id || `${e.source}-${e.target}`,
        source: e.source,
        target: e.target,
        label: e.label,
      }));
      return {
        id: graphId,
        name: mockData.graphData.name,
        description: mockData.graphData.description,
        title: mockData.graphData.name,
        tags: ['demo'],
        nodes,
        edges,
      };
    }
  }

  /**
   * 执行图并获取流式结果
   */
  async executeGraph(
    graphId: string,
    inputText: string,
    onNodeUpdate: (output: EnhancedNodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): Promise<() => void> {
    // 优先尝试后端SSE，失败回退mock
    let cleanup: () => void = () => {};
    let fellBackToMock = false;

    const startMock = () => {
      if (fellBackToMock) return;
      fellBackToMock = true;
      cleanup();
      cleanup = this.simulateMockExecution(graphId, onNodeUpdate, onError, onComplete);
    };

    try {
      // 设置当前图
      await request({
        url: `${OBSERVABILITY_BASE_URL}/graph/setCurrentGraph`,
        method: 'POST',
        params: { graphId },
      });

      // 创建增强节点输出流
      const url = new URL(`${OBSERVABILITY_BASE_URL}/graph/node/stream_enhanced`, window.location.origin);
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
        // 流错误时回退到mock
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
        startMock();
      };

      es.addEventListener('message', handleMessage as any);
      es.addEventListener('error', handleError as any);

      cleanup = () => {
        es.removeEventListener('message', handleMessage as any);
        es.removeEventListener('error', handleError as any);
        es.close();
      };
    } catch (e) {
      if (onError) onError(e);
      startMock();
    }

    return () => {
      cleanup();
    };
  }

  /**
   * 模拟mock执行流程
   */
  private simulateMockExecution(
    graphId: string,
    onNodeUpdate: (output: EnhancedNodeOutput) => void,
    onError?: (error: any) => void,
    onComplete?: () => void
  ): () => void {
    let stepIndex = 0;
    let timers: number[] = [];
    
    // 根据graphId获取对应的执行步骤
    const mockData = this.getMockDataByGraphId(graphId);
    const executionSteps = mockData.executionSteps;

    const executeNextStep = () => {
      if (stepIndex >= executionSteps.length) {
        if (onComplete) onComplete();
        return;
      }

      const step = executionSteps[stepIndex];

      const output: EnhancedNodeOutput = {
        node_id: step.nodeId,
        execution_status: this.mapExecutionStatus(step.execution_status),
        data: {
          input: step.input,
          output: step.response,
          summary: step.summary,
        },
        timestamp: new Date().toISOString(),
        is_final: step.is_final,
      } as any;

      onNodeUpdate(output);
      stepIndex++;

      // 模拟延迟
      const t = window.setTimeout(executeNextStep, 1000);
      timers.push(t);
    };

    const t0 = window.setTimeout(executeNextStep, 500);
    timers.push(t0);

    return () => {
      timers.forEach((t) => window.clearTimeout(t));
      timers = [];
    };
  }

  /**
   * 设置当前活跃图
   */
  async setCurrentGraph(graphId: string): Promise<void> {
    try {
      await request({
        url: `${OBSERVABILITY_BASE_URL}/graph/setCurrentGraph`,
        method: 'POST',
        params: { graphId },
      });
    } catch (e) {
      // 回退为静默处理
      console.log(`Setting current graph (fallback mock): ${graphId}`);
    }
  }

  /**
   * 获取当前活跃图
   */
  async getCurrentGraph(): Promise<IGraphData | null> {
    try {
      const response = await request({
        url: `${OBSERVABILITY_BASE_URL}/graph/getCurrentGraph`,
        method: 'GET',
      });
      const data: any = response.data;

      if (!data) return null;

      const nodes = (data?.nodes || []).map((n: any) => ({
        id: n.id,
        type: n.data?.type || n.type || 'node',
        data: n.data || {},
        position: n.position || { x: 100, y: 100 },
      }));
      const edges = (data?.edges || []).map((e: any) => ({
        id: e.id || `${e.source}-${e.target}`,
        source: e.source,
        target: e.target,
        label: e.label,
      }));

      return {
        id: data?.id || data?.graphId || 'current-graph',
        name: data?.name || data?.title || '未命名流程',
        description: data?.description || '',
        title: data?.name || data?.title || '未命名流程',
        tags: data?.tags || [],
        nodes,
        edges,
      };
    } catch (e) {
      return null;
    }
  }
}

// 导出单例实例
const graphDebugService = new GraphDebugService();
export default graphDebugService;