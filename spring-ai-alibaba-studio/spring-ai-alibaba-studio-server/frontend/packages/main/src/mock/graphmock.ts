/**
 * 图形调试 Mock 数据
 */

// 客户反馈分析工作流Mock数据
export const mockFeedbackAnalysisGraph = {
  id: 'demo-graph-1',
  name: '客户反馈分析',
  description: 'AI驱动的客户反馈分析工作流',
  nodes: [
    {
      id: 'start',
      data: { label: '数据输入', type: 'start' },
      position: { x: 100, y: 200 },
    },
    {
      id: 'preprocess',
      data: { label: '数据预处理', type: 'processor' },
      position: { x: 300, y: 200 },
    },
    {
      id: 'sentiment',
      data: { label: '情感分析', type: 'ai' },
      position: { x: 500, y: 150 },
    },
    {
      id: 'category',
      data: { label: '分类标记', type: 'ai' },
      position: { x: 500, y: 250 },
    },
    {
      id: 'summary',
      data: { label: '生成报告', type: 'processor' },
      position: { x: 700, y: 200 },
    },
    {
      id: 'end',
      data: { label: '输出结果', type: 'end' },
      position: { x: 900, y: 200 },
    },
  ],
  edges: [
    { source: 'start', target: 'preprocess', id: 'e1' },
    { source: 'preprocess', target: 'sentiment', id: 'e2' },
    { source: 'preprocess', target: 'category', id: 'e3' },
    { source: 'sentiment', target: 'summary', id: 'e4' },
    { source: 'category', target: 'summary', id: 'e5' },
    { source: 'summary', target: 'end', id: 'e6' },
  ],
};

// 文档处理流水线工作流Mock数据
export const mockDocumentPipelineGraph = {
  id: 'demo-graph-2',
  name: '文档处理流水线',
  description: '自动化文档处理和提取',
  nodes: [
    {
      id: 'start',
      data: { label: '文档上传', type: 'start' },
      position: { x: 100, y: 200 },
    },
    {
      id: 'extract',
      data: { label: '文本提取', type: 'processor' },
      position: { x: 300, y: 200 },
    },
    {
      id: 'ocr',
      data: { label: 'OCR识别', type: 'ai' },
      position: { x: 500, y: 150 },
    },
    {
      id: 'nlp',
      data: { label: 'NLP处理', type: 'ai' },
      position: { x: 500, y: 250 },
    },
    {
      id: 'structure',
      data: { label: '结构化存储', type: 'processor' },
      position: { x: 700, y: 200 },
    },
    {
      id: 'end',
      data: { label: '完成输出', type: 'end' },
      position: { x: 900, y: 200 },
    },
  ],
  edges: [
    { source: 'start', target: 'extract', id: 'e1' },
    { source: 'extract', target: 'ocr', id: 'e2' },
    { source: 'extract', target: 'nlp', id: 'e3' },
    { source: 'ocr', target: 'structure', id: 'e4' },
    { source: 'nlp', target: 'structure', id: 'e5' },
    { source: 'structure', target: 'end', id: 'e6' },
  ],
};

// 默认工作流数据（指向客户反馈分析）
export const mockGraphData = mockFeedbackAnalysisGraph;

// Mock execution step data has been removed
// All execution data now comes from real streaming API endpoints




// 工作流列表Mock数据
export const mockGraphList = [
  {
    id: 'demo-graph-1',
    name: '客户反馈分析',
    description: 'AI驱动的客户反馈分析工作流',
    tags: ['ai', 'analysis', 'feedback'],
    gmt_modified: new Date().toISOString(),
    status: 'ACTIVE',
  },
  {
    id: 'demo-graph-2', 
    name: '文档处理流水线',
    description: '自动化文档处理和提取',
    tags: ['document', 'automation', 'nlp'],
    gmt_modified: new Date(Date.now() - 86400000).toISOString(),
    status: 'ACTIVE',
  },
];



export default {
  mockGraphData,
  mockGraphList,
  mockFeedbackAnalysisGraph,
  mockDocumentPipelineGraph,
};
