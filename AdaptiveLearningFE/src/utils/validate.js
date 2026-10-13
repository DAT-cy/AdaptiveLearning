/**
 * @description Kiểm tra xem đường dẫn đưa vào có phải là một liên kết ngoại tuyến hay không.
 * @param {string} path - Đường dẫn cần kiểm tra.
 * @return {boolean} Trả về `true` nếu đường dẫn là một liên kết ngoại tuyến, `false` nếu không.
 */
export function isExternal(path) {
  //* Biểu thức chính quy để kiểm tra xem đường dẫn có phải là một liên kết ngoại tuyến hay không.
  //* Nó kiểm tra xem đường dẫn có bắt đầu bằng 'https:', 'mailto:' hay 'tel:' hay không.
  //* Biểu thức chính quy này được sử dụng để xác định liệu một liên kết nên mở trong một cửa sổ mới hay không.
  return /^(https?:|mailto:|tel:)/.test(path)
}

//! ----------------------------------------------------------------------------------------------------------

/**
 * @description Kiểm tra xem mật khẩu có ít nhất 6 ký tự hay không
 * @param str
 * @returns {boolean} Trả về `true` nếu mật khẩu có ít nhất 6 ký tự, ngược lại trả về `false`
 */
export function isPassword(str) {
  return str.length >= 6
}

//! ----------------------------------------------------------------------------------------------------------

/**
 * @description Kiểm tra xem giá trị có phải là một chuỗi hay không
 * @param str
 * @returns {boolean} Trả về `true` nếu giá trị là một chuỗi, ngược lại trả về `false`
 */
export function isString(str) {
  return typeof str === 'string' || str instanceof String
}

//! ----------------------------------------------------------------------------------------------------------

/**
 * @description Kiểm tra xem đối tượng có phải là một mảng hay không
 * @param arg
 * @returns {arg is any[]|boolean} Trả về `true` nếu đối tượng là một mảng, ngược lại trả về `false`
 */
export function isArray(arg) {
  if (typeof Array.isArray === 'undefined') {
    return Object.prototype.toString.call(arg) === '[object Array]'
  }
  return Array.isArray(arg)
}

//! ----------------------------------------------------------------------------------------------------------

/**
 * @description Kiểm tra xem có phải là số điện thoại di động hay không
 * @param str
 * @returns {boolean}
 */
export function isPhone(str) {
  const reg = /^1\d{10}$/
  return reg.test(str)
}
