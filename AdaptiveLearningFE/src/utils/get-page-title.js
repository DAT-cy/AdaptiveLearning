import { settings } from '@/config/settings'

const title = settings.title || 'Vue Template'

/**
 ** Hàm tạo tiêu đề cho trang dựa trên tiêu đề của trang và cấu hình mẫu.
 *
 * @param {string} pageTitle - Tiêu đề của trang.
 * @return {string} Tiêu đề được tạo ra.
 */

export default function getPageTitle(pageTitle) {
  if (pageTitle) return `${pageTitle}-${title}`
  return `${title}`
}
