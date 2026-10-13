export const network = {

  // Địa chỉ api mặc định
  baseURL: import.meta.env.VITE_API_KEY?.trim() || '',

  // Cấu hình phương thức nhận dữ liệu từ máy chủ là application/json;charset=UTF-8 hoặc application/x-www-form-urlencoded;charset=UTF-8
  contentType: 'application/json;charset=UTF-8',

  // Thời gian mà thông báo hiện lên trong giao diện
  messageDuration: 3000,

  // Thời gian tối đa của lệnh yêu cầu
  requestTimeout: 5000,

  // Mã xác nhận thành công, hỗ trợ String, Array, int nhiều loại
  successCode: [200, 201, 202, 0],

  // Mã yêu cầu đăng nhập thất bại
  invalidCode: 401,

  // Mã không có quyền truy cập
  noPermissionCode: 403,
}
