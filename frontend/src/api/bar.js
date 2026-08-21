import { del, get, post } from './request'

/**
 * 吧相关接口
 */

/**
 * 吧详情（名称、头像、帖子数、关注数）
 * @param {string} barId
 * @returns {Promise<{name: string, avatarUrl: string, postCount: number, followerCount: number, creatorId: string}>}
 */
export function queryBar(barId) {
  return post(`/bars/v1/${barId}`)
}

export function createBar(data) {
  return post('/bars/v1/create', data)
}

export function deleteBar(barId) {
  return del(`/bars/v1/${barId}`)
}

/**
 * 按名称搜索吧
 * @returns {Promise<{records: object[], nextCursor: string, hasMore: boolean}>}
 */
export function searchBars(keyword, cursor, pageSize) {
  return get('/bars/v1/search', { keyword, cursor, pageSize })
}
