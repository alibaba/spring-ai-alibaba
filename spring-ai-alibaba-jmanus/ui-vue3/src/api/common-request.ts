// 通用请求方法（TypeScript 版，适用于 Vue 项目）

const BASE_URL = '/api/executor'

// 获取详细的执行记录
export async function getDetails(planId: string): Promise<any | null> {
  try {
    const response = await fetch(`${BASE_URL}/details/${planId}`)
    if (response.status === 404) {
      // 404 返回 null
      return null
    }
    if (!response.ok) throw new Error(`获取详细信息失败: ${response.status}`)
    const rawText = await response.text()
    try {
      return JSON.parse(rawText)
    } catch (jsonParseError) {
      throw jsonParseError
    }
  } catch (error: any) {
    // 记录错误但不抛出异常
    return null
  }
}

// 提交用户表单输入
export async function submitFormInput(planId: string, formData: any): Promise<any> {
  const response = await fetch(`${BASE_URL}/submit-input/${planId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(formData)
  })
  if (!response.ok) {
    let errorData
    try {
      errorData = await response.json()
    } catch (e) {
      errorData = { message: `提交表单输入失败: ${response.status}` }
    }
    throw new Error(errorData.message || `提交表单输入失败: ${response.status}`)
  }
  const contentType = response.headers.get('content-type')
  if (contentType && contentType.indexOf('application/json') !== -1) {
    return await response.json()
  }
  return { success: true }
}
