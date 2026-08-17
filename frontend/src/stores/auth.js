import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/auth'

/**
 * 登录态 store
 * token 持久化到 localStorage，与 request.js 请求拦截器读取的 key 一致（satoken）
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('satoken') || '',
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    /**
     * 登录：成功后保存 token
     * @returns {Promise<string>} token
     */
    async login(username, password) {
      const token = await apiLogin({ username, password })
      this.token = token
      localStorage.setItem('satoken', token)
      return token
    },

    /**
     * 注册：后端只返回 userId 不返回 token，注册成功后需跳登录页
     */
    async register(payload) {
      return apiRegister(payload)
    },

    /**
     * 退出登录：无论后端是否成功都清掉本地 token
     */
    async logout() {
      try {
        await apiLogout()
      } finally {
        this.token = ''
        localStorage.removeItem('satoken')
      }
    },
  },
})
