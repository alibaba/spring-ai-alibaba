declare namespace PromptAPI {
  interface GetPromptsParams {
    search?: "accurate" | "blur";
    tag?: string;
    promptKey?: string;
    pageNo?: number;
    pageSize?: number;
  }

  interface GetPromptsResult {
    pageItems: {
      promptKey: string;
      promptDescription: string;
      latestVersion: string;
      tags: string;
      createTime: number;
      updateTime: number;
      latestVersionStatus: "pre" | "release";
    }[];
    pageNumber: number;
    totalPage: number;
    pageSize: number;
    totalCount: number;
  }

  interface GetPromptResult {
    promptKey: string;
    promptDescription: string;
    latestVersion: string;
    tags: string;
    createTime: number;
    updateTime: number;
    latestVersionStatus: "pre" | "release";
  }

  interface PublishPromptParams {
    promptKey: string;
    promptDescription: string;
    tags: string;
  }

  interface PublishPromptResult {
    promptKey: string;
    promptDescription: string;
    latestVersion: string;
    tags: string;
    createTime: number;
    updateTime: number;
  }

  interface UpdatePromptParams {
    promptKey: string;
    promptDescription: string;
    tags: string;
  }

  interface UpdatePromptResult {
    promptKey: string;
    promptDescription: string;
    latestVersion: string;
    tags: string;
    createTime: number;
    updateTime: number;
    latestVersionStatus: "pre" | "release";
  }

  interface DeletePromptParams {
    promptKey: string;
  }


  interface GetPromptVersionsParams {
    promptKey: string;
    pageNo: number;
    pageSize: number
  }

  interface GetPromptVersionsResult {
    pageItems: {
      version: string;
      promptKey: string;
      versionDescription: string;
      createTime: number;
      previousVersion: string;
      status: "release" | "pre";
    }[],
    pageNumber: number,
    totalPage: number,
    pageSize: number,
    totalCount: number
  }

  interface GetPromptVersionParams {
    promptKey: string;
    version: string;
  }

  interface GetPromptVersionResult {
    version: string;
    promptKey: string;
    versionDescription: string;
    template: string;
    variables: string;
    modelConfig: string;
    createTime: number;
    previousVersion: string;
  }

  interface PublishPromptVersionParams {
    promptKey: string;
    version: string;
    versionDescription: string;
    template: string;
    variables: string; // JSON string
    modelConfig: string; // JSON string
    status: "release" | "pre";
  }

  interface PublishPromptVersionResult {
    version: string;
    promptKey: string;
    versionDescription: string;
    createTime: number;
    previousVersion: string;
  }

  interface RunPromptParams {
    sessionId: string;
    promptKey: string;
    version: string;
    template: string;
    variables: string; // JSON string
    modelConfig: string; // JSON string
    message: string;
    newSession: boolean;
  }

  interface RunPromptResult {
    sessionId: string;
    type: string;
    content?: string;
    error?: string;
    messages?: {
      role: string;
      content: string;
      timestamp: number;
    }[];
    messageCount: number;
  }

  interface GetPromptTemplatesParams {
    search?: "accurate" | "blur";
    tag?: string;
    promptTemplateKey: string;
    pageNo: number;
    pageSize: number;
  }

  interface DeletePromptSessionParams {
    sessionId: string;
  }

  interface DeletePromptSessionResult {
    code: number;
    message: string;
    data: boolean;
  }

  interface GetPromptSessionResult {
    sessionId: string;
    promptKey: string;
    version: string;
    template: string;
    variables: string; // JSON string
    modelConfig: Record<string, any>;
    mockTools: {
      toolDefinition: {
        name: string;
        description: string;
        parameters: string; //JSON string
      }
    }[];
    messages: {
      role: string;
      content: string;
      timestamp: number;
    }[];
    createTime: number;
    lastUpdateTime: number;
  }

  interface GetPromptTemplatesResult {
    pageItems: {
      promptTemplateKey: string;
      templateDescription: string;
      tags: string;
    }[];
    pageNumber: number;
    totalPage: number;
    pageSize: number;
    totalCount: number;
  }

  interface GetPromptTemplateResult {
    promptTemplateKey: string;
    templateDescription: string;
    tags: string;
    template: string;
    variables: string;
    modelConfig: string;
  }

  interface GetModelsParams {
    page?: number;
    size?: number;
    name?: string;
    provider?: string;
    status?: number;
  }

  interface GetModelsResult {
    totalCount: number;
    totalPage: number;
    pageNumber: number;
    pageSize: number;
    pageItems: {
      id: number;
      name: string;
      provider: string;
      modelName: string;
      baseUrl: string;
      defaultParameters: Record<string, any>;
      supportedParameters: Record<string, any>[];
      status: number;
      createTime: string;
      updateTime: string;
    }[]
  }
}
