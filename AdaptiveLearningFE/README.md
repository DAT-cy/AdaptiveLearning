# Project of Hoàng Hiếu

custom by Hoàng Hiếu

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Note](#note)

## Giới thiệu

Nhớ cảm ơn Hoàng Hiếu

## Hướng dẫn cài đặt

Để chạy ứng dụng trên máy local:
Clone repository này về máy

```sh
git clone https://github.com/my-basic-code/vue3.git
```

Cài đặt các dependency

```sh
npm install
```

Khởi động server dev

```sh
npm run dev
```

## Cấu trúc thư mục

├── src

    ├── assets

    ├── components (các component được chia sẻ giữa các features)
        │
        ├── Loading.vue
        │
        └── ui /* Các component UI tái sử dụng */
    ├── config /* Thư mục lưu trữ các config của dự án */
    │
    ├── features /* Chia page theo tính năng và lưu trữ ở đây*/
        │
        └── Todo
            │
            ├── components (components của feature Todo | Dumb components)
            │
            └── pages (pages của feature Todo | Smart components)
    ├── hooks /* Custom hooks */
    │
    ├── layouts /* Các layout wrapper */
        │
        ├── Default.vue
        │
        ├── OnlyView.vue
        │
        └── components (các components của layout)
    ├── lib /* Hàm sử dụng thư viện */
    │
    ├── locales /* Ngôn ngữ */
    │
    ├── router /* Cấu hình routing */
    │
    ├── services /* Thư mục lưu trữ các hàm call api */
    │
    ├── stores /* Quản lý state */
    │
    └── utils /* Các tiện ích khác */

## Note

### config/setting.js

├── setting.config.js

```js
export const setting = {
  /* Tiêu đề ứng dụng. */
  title: 'Vue Template',

  /* Bản quyền. */
  copyright: 'hoanghieupro',

  //Có hiển thị thông tin bản quyền tùy chỉnh ở cuối trang hay không
  footerCopyright: true,

  //Có hiển thị thanh tiến trình hàng đầu hay không
  progressBar: true,

  /* Số lượng route tối đa được lưu trong bộ nhớ cache. */
  keepAliveMaxNum: 99,

  /* Chế độ router (hash hoặc history). */
  routerMode: 'history',

  /* Danh sách các route không cần token. */
  routesWhiteList: ['/login', '/register', '/404', '/'],

  /* Văn bản hiển thị khi loading. */
  loadingText: 'loading...',

  /* Tên token BE. */
  tokenName: 'accessToken',

  /* Tên key lưu trữ token trong localStorage/sessionStorage/cookie. */
  tokenTableName: 'accessToken',

  /* Vị trí lưu trữ token. ( localStorage,sessionStorage,cookie )*/
  storage: 'localStorage',

  /* Ghi nhớ route hiện tại khi token hết hạn. */
  recordRoute: true,

  /* Ngôn ngữ mặc định. */
  i18n: 'vi',

  /* Bật/tắt chặn truy cập trang khi chưa đăng nhập. */
  loginInterception: true,

  /* Bật/tắt loading api cho tất cả request.  */
  apiLoading: true,

  /* Danh sách các request cần thêm loading và chống submit trùng lặp nếu apiLoading la false */
  debounce: [''],
}
export const setting = {
  /* Tiêu đề ứng dụng. */
  title: 'Vue Template',

  /* Bản quyền. */
  copyright: 'hoanghieupro',

  //Có hiển thị thông tin bản quyền tùy chỉnh ở cuối trang hay không
  footerCopyright: true,

  //Có hiển thị thanh tiến trình hàng đầu hay không
  progressBar: true,

  /* Số lượng route tối đa được lưu trong bộ nhớ cache. */
  keepAliveMaxNum: 99,

  /* Chế độ router (hash hoặc history). */
  routerMode: 'history',

  /* Danh sách các route không cần token. */
  routesWhiteList: ['/login', '/register', '/404', '/'],

  /* Văn bản hiển thị khi loading. */
  loadingText: 'loading...',

  /* Tên token BE. */
  tokenName: 'accessToken',

  /* Tên key lưu trữ token trong localStorage/sessionStorage/cookie. */
  tokenTableName: 'accessToken',

  /* Vị trí lưu trữ token. ( localStorage,sessionStorage,cookie )*/
  storage: 'localStorage',

  /* Ghi nhớ route hiện tại khi token hết hạn. */
  recordRoute: true,

  /* Ngôn ngữ mặc định. */
  i18n: 'vi',

  /* Bật/tắt chặn truy cập trang khi chưa đăng nhập. */
  loginInterception: true,

  /* Bật/tắt loading api cho tất cả request.  */
  apiLoading: true,

  /* Danh sách các request cần thêm loading và chống submit trùng lặp nếu apiLoading la false */
  debounce: [''],
}
```

├── net.config.js

```js
export const network = {
  // Địa chỉ api mặc định
  baseURL: import.meta.env.VITE_API_KEY,

  // Cấu hình phương thức nhận dữ liệu từ máy chủ là application/json;charset=UTF-8 hoặc application/x-www-form-urlencoded;charset=UTF-8
  contentType: 'application/json;charset=UTF-8',

  // Thời gian mà thông báo hiện lên trong giao diện
  messageDuration: 3000,

  // Thời gian tối đa của lệnh yêu cầu
  requestTimeout: 5000,

  // Mã xác nhận thành công, hỗ trợ String, Array, int nhiều loại
  successCode: [200, 0],

  // Mã yêu cầu đăng nhập thất bại
  invalidCode: 401,

  // Mã không có quyền truy cập
  noPermissionCode: 403,
}
```

### config/permission.js Thanh tiến trình và Middleware xác thực

Phần mã này cung cấp tính năng thanh tiến trình sử dụng NProgress và triển khai middleware xác thực cho router trong ứng dụng Vue.js. Nó đảm bảo hiển thị thanh tiến trình trong quá trình điều hướng và xử lý xác thực người dùng.

```js
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

const { loginInterception, progressBar, recordRoute, routesWhiteList } =
  settings

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
      next({ path: '/' })
      progressBar && NProgress.done()
    } else {
      // Kiểm tra xem người dùng có các quyền cần thiết hay không
      let hasGetUserInfo = userStore.getterName

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
```

### config/i18n

```js
import { createI18n } from 'vue-i18n'
import en from '../locales/en'
import { settings } from '@/config/settings'
import vi from '../locales/vi'

const { i18n } = settings

/**
 * Tạo một đối tượng Vue I18n và cấu hình nó với ngôn ngữ mặc định,
 * ngôn ngữ dự phòng và tin nhắn dịch.
 *
 * @returns {Object} Đối tượng Vue I18n.
 */
export default createI18n({
  /**
   * Ngôn ngữ mặc định để sử dụng.
   */
  locale: i18n,

  /**
   * Ngôn ngữ để sử dụng nếu ngôn ngữ được yêu cầu không có sẵn.
   */
  fallbackLocale: i18n,

  /**
   * Chế độ thừa kế (legacy mode).
   */
  legacy: false,

  /**
   * Kiểm tra nếu phải điều khiển Vue I18n vào ứng dụng Vue tại toàn cầu.
   */
  globalInjection: true,

  /**
   * Đối tượng chứa tin nhắn dịch cho các ngôn ngữ khác nhau.
   * Các khóa của đối tượng là các mã ngôn ngữ, và các giá trị là tin nhắn dịch
   * cho ngôn ngữ đó.
   */
  messages: {
    /**
     * Tin nhắn dịch cho ngôn ngữ tiếng Anh.
     */
    en,

    /**
     * Tin nhắn dịch cho ngôn ngữ tiếng Việt.
     */
    vi,
  },
})
```

### router/index.js

Setting layout và title

```js
/**
 * name:'router-name'          tên được sử dụng bởi <keep-alive> (phải được đặt!!!)
 * meta : {
    title: 'title'             tên hiển thị trong tiêu đề (nên đặt)
    layout : "default"          tên của layout
  }
 */

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    meta: {
      title: 'Login',
      layout: 'only-view',
    },
    component: loadView('Auth', 'Login'),
  },
]
```
