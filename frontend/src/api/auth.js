import { get, post } from './request'

/**
 * 认证相关接口
 * 后端返回：code="200" 时 data 已由 request 拦截器解包
 */

/**
 * 登录
 * @param {{username: string, password: string}} data
 * @returns {Promise<string>} token（Sa-Token，后续请求头 satoken 携带）
 */
export function login(data) {
  return post('/users/v1/login', data)
}

/**
 * 注册
 * @param {{username: string, password: string, confirmPassword: string, nickname?: string, email?: string, sex?: number}} data
 * @returns {Promise<string>} userId
 */
export function register(data) {
  return post('/users/v1/register', data)
}

/**
 * 退出登录
 * @returns {Promise<void>}
 */
export function logout() {
  return post('/users/v1/logout')
}

/**
 * 查询当前登录用户信息
 * @returns {Promise<{id: string, username: string, nickname: string, sex: number, avatarUrl: string, postCount: number, commentCount: number, followerCount: number}>}
 */
export function getCurrentUser() {
  return get('/users/v1/me')
}
