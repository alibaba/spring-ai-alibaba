
export class DirectApiService {
  private static readonly BASE_URL = '/api/executor'

  // 直接发送任务（直接执行模式）
  public static async sendMessage(query: string): Promise<any> {
    const response = await fetch(`${this.BASE_URL}/execute`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query })
    })
    if (!response.ok) throw new Error(`API请求失败: ${response.status}`)
    return await response.json()
  }
}
