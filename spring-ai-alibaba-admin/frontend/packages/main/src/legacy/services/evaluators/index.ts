import { request } from '@/request';
import { API_PATH } from '../const';

// 创建测评集
export async function createDataset(params: EvaluatorsAPI.CreateDatasetParams) {
  return request({
    url: `${API_PATH}/dataset/dataset`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 获取测评集列表
// /dataset/datasets
export async function getDatasets(params: EvaluatorsAPI.GetDatasetsParams) {
  return request({
    url: `${API_PATH}/dataset/datasets`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取测评集详情
// /dataset/dataset
export async function getDataset(params: EvaluatorsAPI.GetDatasetParams) {
  return request({
    url: `${API_PATH}/dataset/dataset`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 更新测评集
// /dataset/dataset
export async function updateDataset(params: EvaluatorsAPI.UpdateDatasetParams) {
  return request({
    url: `${API_PATH}/dataset/dataset`,
    method: 'PUT',
    data: params,
  }.then(r => r.data);
}

// 删除测评集
// DELETE /api/dataset/dataset
export async function deleteDataset(params: EvaluatorsAPI.DeleteDatasetParams) {
  return request({
    url: `${API_PATH}/dataset/dataset?datasetId=${params.datasetId}`,
    method: 'DELETE',
    data: params,
  }.then(r => r.data);
}

// -------------- 测评集数据项管理接口 --------------

// 创建数据项
// POST /api/dataset/dataItem
export async function createDatasetDataItem(params: EvaluatorsAPI.CreateDatasetDataItemParams) {
  return request({
    url: `${API_PATH}/dataset/dataItem`,
    method: 'POST',
    data: params,
  }).then(r => r.data);
}

// 获取数据项列表
// GET /api/dataset/dataItems
export async function getDatasetDataItems(params: EvaluatorsAPI.GetDatasetDataItemsParams) {
  return request({
    url: `${API_PATH}/dataset/dataItems`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

//获取数据项详情
// GET /api/dataset/dataItem
export async function getDatasetDataItem(params: EvaluatorsAPI.GetDatasetDataItemParams) {
  return request({
    url: `${API_PATH}/dataset/dataItem`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 更新数据项
// PUT /api/dataset/dataItem
export async function updateDatasetDataItem(params: EvaluatorsAPI.UpdateDatasetDataItemParams) {
  return request({
    url: `${API_PATH}/dataset/dataItem`,
    method: 'PUT',
    data: params,
  }.then(r => r.data);
}

// 删除数据项
// DELETE /api/dataset/dataItem
export async function deleteDatasetDataItem(params: EvaluatorsAPI.DeleteDatasetDataItemParams) {
  return request({
    url: `${API_PATH}/dataset/dataItem`,
    method: 'DELETE',
    data: params,
  }.then(r => r.data);
}

// 批量删除数据项
// DELETE /api/dataset/dataItems
export async function deleteDatasetDataItems(params: EvaluatorsAPI.DeleteDatasetDataItemsParams) {
  return request({
    url: `${API_PATH}/dataset/dataItems`,
    method: 'DELETE',
    data: params,
  }.then(r => r.data);
}

// 从Trace添加数据项到数据集
// POST /api/dataset/dataItemFromTrace
export async function createDatasetDataItemFromTrace(params: EvaluatorsAPI.CreateDatasetDataItemFromTraceParams) {
  return request({
    url: `${API_PATH}/dataset/dataItemFromTrace`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// -------------- 测评集版本管理接口 --------------

// 创建测评集版本
// POST /api/dataset/datasetVersion
export async function createDatasetVersion(params: EvaluatorsAPI.CreateDatasetVersionParams) {
  return request({
    url: `${API_PATH}/dataset/datasetVersion`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 获取测评集版本列表
// GET /api/dataset/datasetVersions
export async function getDatasetVersions(params: EvaluatorsAPI.GetDatasetVersionsParams) {
  return request({
    url: `${API_PATH}/dataset/datasetVersions`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 更新测评集版本
// PUT /api/dataset/datasetVersion
export async function updateDatasetVersion(params: EvaluatorsAPI.UpdateDatasetVersionParams) {
  return request({
    url: `${API_PATH}/dataset/datasetVersion`,
    method: 'PUT',
    data: params,
  }.then(r => r.data);
}


// -------------- 评估器管理接口 --------------

// 创建评估器
// POST /api/evaluator
export async function createEvaluator(params: EvaluatorsAPI.CreateEvaluatorParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluator`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 创建评估器版本
// POST /api/evaluatorVersion
export async function createEvaluatorVersion(params: EvaluatorsAPI.CreateEvaluatorVersionParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluatorVersion`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 获取评估器列表
// GET /api/evaluators
export async function getEvaluators(params: EvaluatorsAPI.GetEvaluatorsParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluators`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取评估器详情
// GET /api/evaluator
export async function getEvaluator(params: EvaluatorsAPI.GetEvaluatorParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluator`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 更新评估器
// PUT /api/evaluator
export async function updateEvaluator(params: EvaluatorsAPI.UpdateEvaluatorParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluator`,
    method: 'PUT',
    data: params,
  }.then(r => r.data);
}

// 删除评估器
// DELETE /api/evaluator
export async function deleteEvaluator(params: EvaluatorsAPI.DeleteEvaluatorParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluator`,
    method: 'DELETE',
    params: params,
  }.then(r => r.data);
}

// 调试评估器
// POST /api/evaluator/debug
export async function debugEvaluator(params: EvaluatorsAPI.DebugEvaluatorParams) {
  return request({
    url: `${API_PATH}/evaluator/debug`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 获取评估模板列表
// GET /api/evaluator/templates
export async function getEvaluatorTemplates() {
  return request({
    url: `${API_PATH}/evaluator/templates`,
    method: 'GET',
  }.then(r => r.data);
}

// 获取评估模板详情
// GET /api/evaluator/templates
export async function getEvaluatorTemplate(params: { templateId: number }) {
  return request({
    url: `${API_PATH}/evaluator/template`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取评估器版本列表
export async function getEvaluatorVersions(params: EvaluatorsAPI.GetEvaluatorVersionsParams) {
  return request({
    url: `${API_PATH}/evaluator/evaluatorVersions`,
    method: 'GET',
    params,
  }.then(r => r.data);
}


// 获取评估器关联的实现列表
// GET /api/evaluator/experiments
export async function getEvaluatorExperiments(params: EvaluatorsAPI.GetEvaluatorExperimentsParams) {
  return request({
    url: `${API_PATH}/evaluator/experiments`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取评估器版本详情
// export async function getEvaluatorVersion(params: EvaluatorsAPI.GetEvaluatorVersionParams) {
//   return request<EvaluatorsAPI.GetEvaluatorVersionResult>(`${API_PATH}/evaluator/version`, {
//     method: 'GET',
//     params,
//   });
// }

// -------------- 实验管理接口 --------------

// 创建实验
// POST /api/experiment
export async function createExperiment(params: EvaluatorsAPI.CreateExperimentParams) {
  return request({
    url: `${API_PATH}/experiment`,
    method: 'POST',
    data: params,
  }.then(r => r.data);
}

// 获取实验列表
// GET /api/experiments
export async function getExperiments(params: EvaluatorsAPI.GetExperimentsParams) {
  return request({
    url: `${API_PATH}/experiments`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取实验详情
// GET /api/experiment
export async function getExperiment(params: EvaluatorsAPI.GetExperimentParams) {
  return request({
    url: `${API_PATH}/experiment`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 获取实验结果
// GET /api/experiment/result
export async function getExperimentResult(params: EvaluatorsAPI.GetExperimentResultParams) {
  return request({
    url: `${API_PATH}/experiment/result`,
    method: 'GET',
    params,
  }.then(r => r.data);
}

// 停止实验
// PUT /api/experiment/stop
export async function stopExperiment(params: EvaluatorsAPI.StopExperimentParams) {
  return request({
    url: `${API_PATH}/experiment/stop?experimentId=${params.experimentId}`,
    method: 'PUT',
    data: params,
  }.then(r => r.data);
}

// 删除实验
// DELETE /api/experiment
export async function deleteExperiment(params: EvaluatorsAPI.DeleteExperimentParams) {
  return request({
    url: `${API_PATH}/experiment?experimentId=${params.experimentId}`,
    method: 'DELETE',
    data: params,
  }.then(r => r.data);
}

// 获取实验概览结果
// GET /api/experiment/overview
export async function getExperimentOverview(params: EvaluatorsAPI.GetExperimentOverviewParams) {
  return request({
    url: `${API_PATH}/experiment/results`,
    method: 'GET',
    params,
  }.then(r => r.data);
}