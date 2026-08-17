/**
 * 展示格式化工具
 */

/**
 * 时间格式化：后端返回 LocalDateTime（如 2026-08-10T09:30:00）
 * 简单转成 "2026-08-10 09:30"，兼容 null
 */
export function formatTime(time) {
  if (!time) return ''
  return String(time).replace('T', ' ').slice(0, 16)
}

/**
 * 数字友好显示：1000 -> 1k，1000000 -> 100w
 */
export function formatCount(num) {
  const n = Number(num) || 0
  if (n >= 100000000) return (n / 100000000).toFixed(1).replace(/\.0$/, '') + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(n)
}
