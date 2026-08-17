import { get } from './request'

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
 *                 barName, barAvatarUrl, barPostCount, barFollowerCount }
 */
export function getFeed(cursor, pageSize) {
  return get('/posts/v1/feed', { cursor, pageSize })
}

/**
 * 帖子详情（帖子 + 所属吧 + 作者，不含楼层）
 * @param {string} postId
 * @returns {Promise<PostDetailPageVO>}
 *   PostDetailPageVO: { postId, barId, barName, barAvatarUrl, title, content, coverImage,
 *                       viewCount, likeCount, commentCount, favoriteCount, lastFloor,
 *                       authorUserId, authorNickname, authorAvatarUrl, createTime }
 */
export function getPostDetail(postId) {
  return get('/posts/v1/detail', { postId })
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
  return get('/posts/v1/floors', { postId, pageNum, pageSize })
}
