declare namespace EvaluatorsAPI {
  interface CreateDatasetParams {
    name: string;
    description?: string;
    columnsConfig: {
      name: string;
      dataType: string;
      displayFormat: string;
      description?: string;
      required?: true
    }[]
  }

  interface CreateDatasetResult {
    id: number;
    name: string;
    description: string;
    columnsConfig: string;
    dataCount: number;
    createTime: string;
    updateTime: string;
    versions: any;
    experiments: any;
  }

  interface GetDatasetsParams {
    pageNumber?: number;
    pageSize?: number;
    datasetName?: string;
  }

  interface GetDatasetsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      description: string;
      columnsConfig: string;
      createTime: string;
      updateTime: string;
      dataCount?: number; // 数据量（可能需要从其他地方获取）
      version?: string; // 版本信息（可能需要从其他地方获取）
    }[]
  }

  interface GetDatasetParams {
    datasetId: number;
  }

  interface GetDatasetResult {
    id: number;
    name: string;
    description: string;
    columnsConfig: string;
    dataCount: number;
    createTime: string;
    updateTime: string;
    versions: any;
    experiments: any;
  }

  interface GetDatasetExperimentsParams {
    pageNumber?: number;
    pageSize?: number;
    datasetId?: number;
  }

  interface GetDatasetExperimentsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      description: string;
      datasetId: number;
      datasetVersion: string;
      evaluationObjectConfig: string;
      evaluatorConfig: string;
      status: string,
      progress: number,
      completeTime: string,
      createTime: string;
      updateTime: string
    }[]
  }

  interface UpdateDatasetParams {
    datasetId: number | string;
    name: string;
    description?: string;
    columnsConfig?: Array<{
      name: string;
      dataType: string;
      displayFormat: string;
      description?: string;
      required?: boolean;
    }>;
  }

  interface UpdateDatasetResult {
    id: number;
    name: string;
    description: string;
    updateTime: string;
  }

  interface DeleteDatasetParams {
    datasetId: number;
  }

  interface DeleteDatasetResult {
    code: number;
    message: string;
  }

  interface CreateDatasetDataItemParams {
    datasetId: number;
    dataContent: string | string[]; // 支持单个字符串或字符串数组
    remark?: string;
    columnsConfig?: Array<{
      name: string;
      dataType: string;
      displayFormat: string;
      description?: string;
      required?: boolean;
    }>;
  }

  interface CreateDatasetDataItemResult {
    id: number;
    datasetId: number;
    dataContent: string;
    remark: string;
    createTime: string;
    updateTime: string;
    deleted: boolean;
  }

  // 批量创建数据项的返回结果
  interface BatchCreateDatasetDataItemResult {
    code: number;
    message: string;
    data: CreateDatasetDataItemResult[];
  }

  interface GetDatasetDataItemsParams {
    datasetVersionId?: number | string;
    pageNumber?: number;
    pageSize?: number;
  }

  interface GetDatasetDataItemsResult {
    code: number;
    message: string;
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      datasetId: number;
      dataContent: string;
      remark: string;
      createTime: string;
      updateTime: string;
    }[]
  }

  interface GetDatasetDataItemParams {
    id: number;
  }

  interface GetDatasetDataItemResult {
    id: number;
    datasetId: number;
    dataContent: string;
    remark: string;
    createTime: string;
    updateTime: string;
    deleted: boolean;
  }

  interface UpdateDatasetDataItemParams {
    id: number;
    dataContent: string;
    remark?: string;
  }

  interface UpdateDatasetDataItemResult {
    id: number;
    datasetId: number;
    dataContent: string;
    remark: string;
    updateTime: string;
  }

  interface DeleteDatasetDataItemParams {
    id: number;
  }

  interface DeleteDatasetDataItemResult {
    code: number;
    message: string;
  }

  interface DeleteDatasetDataItemsParams {
    ids: number[];
  }

  interface DeleteDatasetDataItemsResult {
    code: number;
    message: string;
  }

  // 从Trace添加数据项到数据集
  interface CreateDatasetDataItemFromTraceParams {
    datasetId: number;
    datasetVersionId: number;
    dataContent: string[];
    columnsConfig: Array<{
      name: string;
      dataType: string;
      displayFormat: string;
      description?: string;
      required: boolean;
    }>;
  }

  interface CreateDatasetDataItemFromTraceResult {
    code: number;
    message: string;
    data: any;
  }

  interface CreateDatasetVersionParams {
    datasetId: number | string;
    description?: string;
    columnsConfig: Array<{
      name: string;
      dataType: string;
      displayFormat: string;
      description?: string;
      required?: boolean;
    }>;
    datasetItems: number[];
    status: string;
  }

  interface CreateDatasetVersionResult {
    id: number;
    datasetId: number;
    version: string;
    description: string;
    createTime: string;
    updateTime: string;
    deleted: boolean;
  }

  interface GetDatasetVersionsParams {
    pageNumber?: number;
    pageSize?: number;
    datasetId: number;
  }

  interface GetDatasetVersionsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      datasetId: number | string;
      version: string;
      description: string;
      dataCount: number;
      createTime: string;
      columnsConfig: Array<{
        name: string;
        dataType: string;
        displayFormat: string;
        description?: string;
        required?: boolean;
      }>;
      status: string;
      experiments: string;
      datasetItems: string;
    }[]
  }

  interface UpdateDatasetVersionParams {
    id: number;
    version: string;
    description?: string;
  }

  interface UpdateDatasetVersionResult {
    id: number;
    version: string;
    description: string;
    updateTime: string;
  }

  interface CreateEvaluatorParams {
    name: string;
    description?: string;
  }

  interface CreateEvaluatorResult {
    id: number;
    name: string;
    description: string;
    createTime: string;
    updateTime: string;
  }

  interface CreateEvaluatorVersionParams {
    evaluatorId: string;
    description: string;
    modelConfig: string;
    prompt: string;
    version: string;
    variables: string;
  }

  interface CreateEvaluatorVersionResult {
    id: number;
    evaluatorId: number;
    version: string;
    description: string;
    prompt: string;
    createTime: string;
    updateTime: string;
    deleted: boolean;
  }

  interface GetEvaluatorsParams {
    pageNumber?: number;
    pageSize?: number;
    name?: string;
  }

  interface GetEvaluatorsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      description: string;
      createTime: string;
      updateTime: string;
      modelName: string;
      prompt?: string;
      latestVersion?: string;
      modelConfig?: string; // JSON String
    }[]
  }

  interface GetEvaluatorParams {
    id: number;
  }

  interface GetEvaluatorResult {
    id: number;
    name: string;
    description: string;
    prompt: string;
    latestVersion: string;
    createTime: string;
    updateTime: string;
    modelConfig?: string;
    variables?: string;
  }

  interface UpdateEvaluatorParams {
    id: number;
    name: string;
    description?: string;
  }

  interface UpdateEvaluatorResult {
    id: number;
    name: string;
    description: string;
    updateTime: string;
  }

  interface DeleteEvaluatorParams {
    id: number;
  }

  interface DeleteEvaluatorResult {
    code: number;
    message: string;
  }

  interface DebugEvaluatorParams {
    modelConfig: string;
    prompt: string;
    variables: string;
  }

  interface DebugEvaluatorResult {
    score: number;
    reason: string;
    evaluationTime: string;
  }

  interface GetEvaluatorTemplatesResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      evaluatorTemplateKey: string;
      templateDesc: string;
      template: string;
      variables: string;
      modelConfig: string;
    }[]
  }

  interface GetEvaluatorTemplateResult {
    id: number;
    evaluatorTemplateKey: string;
    templateDesc: string;
    template: string;
    variables: string;
    modelConfig: string;
  }

  interface GetEvaluatorVersionsParams {
    evaluatorId: number;
    pageNumber?: number;
    pageSize?: number;
    name?: string;
  }

  interface GetEvaluatorVersionsResult {
    totalCount: number,
    totalPage: number,
    pageNumber: number,
    pageSize: number,
    pageItems: {
      id: number,
      description: string,
      version: string,
      template: string;
      modelConfig: string; // JSON string
      variables: string, // JSON string
      status: string,
      experiments: string,
      createTime: string,
      updateTime: string
    }[]
  }

  interface GetEvaluatorVersionParams {
    evaluatorId: number;
    version: string;
  }

  interface GetEvaluatorVersionResult {
    id: number,
    description: string,
    latestVersion: string,
    prompt?: string;
    modelConfig?: string; // JSON string
    variables?: string, // JSON string
    createTime: string,
    updateTime: string
  }

  interface CreateExperimentParams {
    name: string;
    description?: string;
    datasetId: number;
    datasetVersionId: string | number;
    datasetVersion?: string;
    evaluationObjectConfig: string;
    evaluatorConfig: string;
  }

  interface CreateExperimentResult {
    code: number;
    message: string;
    data: {
      id: number;
      name: string;
      description: string;
      datasetId: number;
      datasetVersionId: string;
      evaluationObjectConfig: string;
      evaluatorVersionIds: number[];
      status: string;
      createTime: string;
      updateTime: string;
    }
  }

  interface GetExperimentsParams {
    pageNumber?: number;
    pageSize?: number;
    name?: string;
    status?: string;
  }

  interface GetExperimentsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      description: string;
      datasetId: string;
      evaluatorId: string;
      status: string;
      createTime: string;
    }[]
  }

  interface GetExperimentParams {
    experimentId: number;
  }

  interface GetExperimentResult {
    code: number;
    message: string;
    data: {
      id: number;
      name: string;
      description: string;
      datasetId: number;
      datasetVersion: string;
      evaluationObjectConfig: string;
      evaluatorVersionIds: number[];
      status: string;
      progress: number;
      completeTime: string;
      createTime: string;
      updateTime: string;
    }
  }

  interface GetExperimentResultParams {
    experimentId: number;
    evaluatorVersionId: number;
    pageNumber: number;
    pageSize: number;
  }

  interface GetExperimentResultResult {
    code: number;
    message: string;
    data: {
      totalCount: number;
      totalPage: number;
      pageNumber: number;
      pageSize: number;
      pageItems: {
        id: number;
        input: string;
        actualOutput: string;
        referenceOutput: string;
        score: number;
        reason: string;
        createTime: string;
      }[];
    };
  }

  interface StopExperimentParams {
    experimentId: number;
  }

  interface StopExperimentResult {
    id: number;
    status: string;
    updateTime: string;
  }

  interface DeleteExperimentParams {
    experimentId: number;
  }

  interface DeleteExperimentResult {
    code: number;
    message: string;
  }

  interface GetExperimentOverviewParams {
    experimentId: number;
  }

  interface GetExperimentOverviewResult {
    code: number;
    message: string;
    data: {
      experimentId: number;
      averageScore: number;
      evaluatorVersionId: number;
      progress: number;
      completeItemsCount: number;
      totalItemsCount: number; // 添加总数据项数量字段
    }[];
  }

  interface GetEvaluatorExperimentsParams {
    evaluatorId: number;
    pageNumber?: number;
    pageSize?: number;
  }

  interface GetEvaluatorExperimentsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      description: string;
      datasetId: number;
      datasetVersion: string;
      evaluationObjectConfig: string;
      evaluatorConfig: string;
      status: string;
      progress: number;
      completeTime: string;
      createTime: string;
      updateTime: string;
    }[]
  }
}