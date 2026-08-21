import { del, get, post, put } from './request'

/**
 * 帖子相关接口（信息展示阶段，全部匿名可访问）
 * 数据字段与后端 VO 对齐，命名保持一致，方便对照
 */

/**
 * 首页帖子流（游标分页，用于无限滚动）
 * @param {string} cursor 下一页游标（热度分），首次为空
 * @param {number} [pageSize] 每页条数
 * @returns {Promise<{records: FeedPostVO[], nextCursor: string, hasMore: boolean}>}
 *   FeedPostVO: { postId, title, content, coverImage, barId, hotScore,
 *                 favoriteCount, commentCount, likeCount,
 *                 barName, barAvatarUrl, barPostCount, barFollowerCount }
 */
export function getFeed(cursor, pageSize) {
  return get('/posts/v1/home/feed', { cursor, pageSize })
}

/**
 * 按标题或正文搜索帖子
 * @returns {Promise<{records: object[], nextCursor: string, hasMore: boolean}>}
 */
export function searchPosts(keyword, cursor, pageSize) {
  return get('/posts/v1/search', { keyword, cursor, pageSize })
}

/**
 * 吧主页帖子流（游标分页，支持按时间/热度排序）
 * @param {string} barId 吧 id
 * @param {{orderBy?: 'hot'|'createTime', pageSize?: number, cursor?: object|null}} body
 *   orderBy 缺省按 createTime；切换排序时必须把 cursor 置空
 * @returns {Promise<{cursor: object, data: BarPostCardVO[], hasNext: boolean}>}
 *   BarPostCardVO: { postId, title, content, viewCount, likeCount, commentCount,
 *                    lastReplyTime, createTime, hotScore, userId, nickName, avatarUrl }
 */
export function getBarFeed(barId, body) {
  return post('/posts/v1/bars/home/posts', { ...body, barId })
}

export function getPostSummary(postId) {
  return get('/posts/v1/query', { postId })
}

export function createPost(barId, data) {
  return post('/posts/v1/create', { ...data, barId })
}

export function updatePost(barId, data) {
  return put(`/posts/v1/${barId}/delete`, data)
}

export function deletePost(barId, postId) {
  return del(`/posts/v1/${barId}/delete`, { postId })
}

/**
 * 楼层分页（顶层楼层 + 楼中楼 children 子树）
 * @param {string} postId
 * @param {number} [pageNum] 页码，从 1 开始
 * @param {number} [pageSize] 每页楼层数
 * @returns {Promise<{records: FloorVO[], total, totalPages, pageNum, pageSize}>}
 *   FloorVO: { id, floor, userId, nickname, avatarUrl, content, likeCount,
 *              createTime, children: FloorVO[] }
 */
export function getFloors(postId, pageNum, pageSize) {
  return get('/comments/v1/page', { postId, pageNum, pageSize })
}

export function getReplies(postId, rootId, pageNum, pageSize) {
  return get('/comments/v1/replies/page', { postId, rootId, pageNum, pageSize })
}

export function createComment(postId, data) {
  return post(`/posts/v1/${postId}/comments`, data)
}

export function deleteComment(commentId) {
  return del(`/posts/v1/comments/${commentId}/delete`)
}

export function likeComment(commentId) {
  return post(`/posts/v1/comments/${commentId}/like`)
}

export function unlikeComment(commentId) {
  return del(`/posts/v1/comments/${commentId}/unlike`)
}
