declare namespace ModelAPI {
  interface GetModelListResult {
    id: string
    provider: string,
    maxTokens: number,
    capabilities: string[],
    name: string
  }
}