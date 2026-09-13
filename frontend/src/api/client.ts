import axios, { type AxiosError } from 'axios'

/** 后端统一响应包装。 */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
  timestamp: string
}

const client = axios.create({
  // 开发环境由 Vite 代理到后端 8080，生产环境前后端同源，因此统一用相对路径
  baseURL: '/api',
  timeout: 10000,
})

/** 把后端的错误信息提取成直接可展示的文案。 */
function toMessage(error: AxiosError<ApiResponse<unknown>>): string {
  const fromBody = error.response?.data?.message
  if (fromBody) return fromBody
  if (error.response) return `请求失败（HTTP ${error.response.status}）`
  if (error.code === 'ECONNABORTED') return '请求超时，后端可能没有启动'
  return '无法连接后端，请确认服务已在 8080 端口运行'
}

/** GET，自动拆掉 ApiResponse 外壳。 */
export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  try {
    const response = await client.get<ApiResponse<T>>(url, { params })
    return unwrap(response.data)
  } catch (error) {
    throw new Error(toMessage(error as AxiosError<ApiResponse<unknown>>))
  }
}

/** POST，自动拆掉 ApiResponse 外壳。 */
export async function post<T>(url: string, body?: unknown): Promise<T> {
  try {
    const response = await client.post<ApiResponse<T>>(url, body)
    return unwrap(response.data)
  } catch (error) {
    throw new Error(toMessage(error as AxiosError<ApiResponse<unknown>>))
  }
}

function unwrap<T>(payload: ApiResponse<T>): T {
  if (!payload.success) {
    throw new Error(payload.message ?? '请求失败')
  }
  return payload.data
}

export default client
