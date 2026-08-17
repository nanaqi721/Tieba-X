import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 统一封装：前端所有请求的唯一出口
 *
 * 后端统一响应结构：{ code: "200", data, message }，code 是字符串
 * - 拦截器把 data 解包出来直接返回，页面拿到即业务数据
 * - code !== "200" 时弹出后端错误信息并 reject
 */

// 创建 axios 实例
const service = axios.create({
  // baseURL 来自 .env.development 的 VITE_API_BASE=/api，经 Vite dev proxy 转发
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
})

// 请求拦截器：携带登录态 token（信息展示阶段不需要登录，预留）
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) {
    // 后端 Sa-Token 约定的请求头名
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截器：解包 Result 包装
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端约定成功码是字符串 "200"
    if (res.code === '200') {
      return res.data
    }
    // 业务失败：弹出后端给的错误信息
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // HTTP 层错误
    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/**
 * 请求方法封装：get / post / delete / put
 * 返回已解包的 data，页面直接 await 使用
 */
export function get(url, params) {
  return service.get(url, { params })
}

export function post(url, data) {
  return service.post(url, data)
}

export function del(url, params) {
  return service.delete(url, { params })
}

export function put(url, data) {
  return service.put(url, data)
}

export default service
