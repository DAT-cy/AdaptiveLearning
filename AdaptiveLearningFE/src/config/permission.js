import 'nprogress/nprogress.css' // Kiểu thanh tiến trình

import NProgress from 'nprogress' // Thanh tiến trình
import getPageTitle from '@/utils/get-page-title'
import { getToken } from '@/utils/auth' // Lấy token từ cookie
import router from '../router'
import { settings } from '@/config/settings'
import { useUserStore } from '@/stores/user'

/**
 * Cấu hình NProgress
 *
 * @property {string} easing - Hình dạng eazing được sử dụng cho thanh tiến trình
 * @property {number} speed - Tốc độ nâng cao của thanh tiến trình
 * @property {number} trickleSpeed - Tốc độ rộng của thanh tiến trình
 * @property {boolean} showSpinner - Hiển thị spinner hay không
 */
NProgress.configure({
  easing: 'ease',
  speed: 500,
  trickleSpeed: 200,
  showSpinner: false,
})

const { loginInterception, progressBar, recordRoute, routesWhiteList } = settings



/**
  * Chức năng Middleware cho router trước mỗi hook.
  * Kiểm tra xem người dùng có được xác thực hay không và có các quyền cần thiết để truy cập vào một tuyến đường hay không.
  * Chuyển hướng đến trang đăng nhập nếu người dùng không được xác thực hoặc không có các quyền cần thiết.
  *
  * @param {Object} to - Đối tượng tuyến đường mục tiêu.
  * @param {Object} from - Đối tượng tuyến đường trước đó.
  * @param {Function} next - Chức năng phần mềm trung gian tiếp theo.
  */

router.beforeEach(async (to, from, next) => {
  // Bắt đầu thanh tiến trình
  progressBar && NProgress.start()

  // Lấy kho lưu trữ của người dùng
  const userStore = useUserStore()

  // Kiểm tra xem người dùng đã được xác thực chưa
  let hasToken = getToken()

  // Nếu việc chặn đăng nhập bị vô hiệu hóa, hãy coi người dùng là đã được xác thực
  if (!loginInterception) hasToken = true

  if (hasToken) {
    // Nếu tuyến mục tiêu là trang đăng nhập, hãy chuyển hướng đến trang chủ
    if (to.path === '/login') {
      next({ path: '/home' })
      progressBar && NProgress.done()
    } else {
      // Kiểm tra xem người dùng có các quyền cần thiết hay không
      let hasGetUserInfo = userStore.getterUserName

      // Nếu việc chặn đăng nhập bị tắt, hãy coi như người dùng có các quyền cần thiết
      if (!loginInterception) hasGetUserInfo = true

      if (hasGetUserInfo) {
        // Người dùng có các quyền cần thiết, tiến tới tuyến đường đích
        next()
      } else {
        try {
          // Lấy thông tin người dùng
          await userStore.getInfo()
          next()
        } catch (error) {
          // Người dùng không có quyền cần thiết, đặt lại mã thông báo và chuyển hướng đến trang đăng nhập
          userStore.resetToken()
          next(`/login?redirect=${to.path}`)
          progressBar && NProgress.done()
        }
      }
    }
  } else {
    // Người dùng chưa được xác thực
    if (routesWhiteList.indexOf(to.path) !== -1) {
      // Tuyến đường mục tiêu nằm trong danh sách trắng, tiến tới tuyến đường mục tiêu
      next()
    } else {
      if (recordRoute) {
        // Ghi lại lộ trình và chuyển hướng đến trang đăng nhập
        next(`/login?redirect=${to.path}`)
      } else {
        // Chuyển hướng đến trang đăng nhập
        next('/login')
      }
    }
  }
  // Đặt tiêu đề trang
  document.title = getPageTitle(to.meta.title)
})

router.afterEach(() => {
  // Hoàn thành thanh tiến trình
  progressBar && NProgress.done()
})
