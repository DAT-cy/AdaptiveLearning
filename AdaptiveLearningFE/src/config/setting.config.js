export const setting = {
  /* Tiêu đề ứng dụng. */
  title: 'Adaptive Learning',

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
  routesWhiteList: ['/login', '/register', '/verify-email', '/404', '/'],

  /* Tên token BE. */
  tokenName: 'token',

  /* Tên key lưu trữ token trong localStorage/sessionStorage/cookie. */
  tokenTableName: 'token',

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

