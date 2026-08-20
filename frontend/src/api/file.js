import { post } from './request'

/**
 * 批量上传图片，一次最多 5 张。
 * @param {File[]} files
 * @returns {Promise<{url: string}[]>}
 */
export function uploadImages(files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return post('/files/v1/uploads', formData)
}
