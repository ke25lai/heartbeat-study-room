/** 统一 HTTP 封装，携带 Cookie 保持 Session */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const resp = await fetch(url, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  const result: ApiResult<T> = await resp.json()
  if (result.code !== 200) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}

export const api = {
  get: <T>(url: string) => request<T>(url, { method: 'GET' }),
  post: <T>(url: string, body?: unknown) => request<T>(url, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body)
  })
}
