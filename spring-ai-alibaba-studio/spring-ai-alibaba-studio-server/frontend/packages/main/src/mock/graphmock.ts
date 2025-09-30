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

// 客户反馈分析执行步骤Mock数据
export const mockFeedbackAnalysisSteps = [
  {
    nodeId: 'start',
    input: '客户反馈原始数据：包含评论、评分、时间戳等信息',
    response: '成功接收1000条客户反馈数据',
    summary: '📥 数据输入 - 成功加载客户反馈数据',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'preprocess',
    input: '原始反馈文本数据',
    response: '完成数据清洗：去除重复、过滤无效内容、标准化格式',
    summary: '🧹 数据预处理 - 清洗后剩余876条有效数据',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'sentiment',
    input: '预处理后的文本数据',
    response: '情感分析结果：正面52%，中性31%，负面17%',
    summary: '😊 情感分析 - AI模型分析完成，整体情感偏正面',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'category',
    input: '预处理后的文本数据',
    response: '分类结果：产品质量(35%)，服务态度(28%)，价格(20%)，物流(17%)',
    summary: '🏷️ 分类标记 - 识别出4个主要反馈类别',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'summary',
    input: '情感分析和分类结果',
    response: '生成综合分析报告：包含趋势分析、问题识别、改进建议',
    summary: '📊 生成报告 - 完成客户反馈综合分析报告',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'end',
    input: '分析报告',
    response: '输出完整的客户反馈分析报告和可视化图表',
    summary: '🎯 输出结果 - 客户反馈分析工作流完成',
    execution_status: 'SUCCESS',
    is_final: true,
  },
];

// 文档处理流水线执行步骤Mock数据
export const mockDocumentPipelineSteps = [
  {
    nodeId: 'start',
    input: '上传的文档文件：PDF、Word、图片等格式',
    response: '成功接收25个文档文件，总大小156MB',
    summary: '📁 文档上传 - 多格式文档批量上传完成',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'extract',
    input: '各种格式的文档文件',
    response: '成功提取文本内容，识别文档结构和元数据',
    summary: '🔍 文本提取 - 从23个文档中提取出可处理文本',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'ocr',
    input: '图片和扫描文档',
    response: 'OCR识别结果：准确率95.2%，识别出12,847个字符',
    summary: '👁️ OCR识别 - 图像文字识别完成，准确率良好',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'nlp',
    input: '提取的文本内容',
    response: 'NLP处理结果：实体识别、关键词提取、摘要生成',
    summary: '🧠 NLP处理 - 智能文本分析完成，提取关键信息',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'structure',
    input: 'OCR和NLP处理结果',
    response: '结构化数据存储：创建索引、建立关联、标准化格式',
    summary: '🗃️ 结构化存储 - 文档数据已结构化并建立索引',
    execution_status: 'SUCCESS',
    is_final: false,
  },
  {
    nodeId: 'end',
    input: '结构化的文档数据',
    response: '输出处理完成的文档数据库和检索接口',
    summary: '✅ 完成输出 - 文档处理流水线执行完毕',
    execution_status: 'SUCCESS',
    is_final: true,
  },
];

// 默认执行步骤数据（指向客户反馈分析）
export const mockExecutionSteps = mockFeedbackAnalysisSteps;

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
  mockExecutionSteps,
  mockGraphList,
  mockFeedbackAnalysisGraph,
  mockFeedbackAnalysisSteps,
  mockDocumentPipelineGraph,
  mockDocumentPipelineSteps,
};
