/**
 ** Debounce hàm gọi.
 * 
 * @param {Function} fn - Hàm được debounce.
 * @param {number} delay - Thời gian đợi trước khi gọi hàm.
 * @returns {Function} - Hàm debounced.
 */
export function debounce(fn, delay) {
  let timeout;
  /**
   ** Debounced hàm được gọi sau một khoảng thời gian nhất định.
   ** Nếu hàm được gọi trong khoảng thời gian nhất định, thì sẽ
   ** reset timer và bắt đầu tính lại khoảng thời gian.
   * 
   * @param {...*} args - Các đối số được truyền vào hàm.
   */
  return function (...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}

//! ----------------------------------------------------------------------------------------------------------

/**
 ** Gộp nhiều chuỗi thành một chuỗi duy nhất.
 *
 * @param {...string} args - Các chuỗi cần gộp.
 * @return {string} - Chuỗi gộp.
 */
export function stringConcatenation(...args) {
  //* Sử dụng phương thức Array.prototype.join() để gộp các chuỗi thành một chuỗi.
  //* Phương thức này sẽ gộp các chuỗi thành một chuỗi duy nhất bằng cách sử dụng chuỗi không khoảng trống làm nền.
  return args.join('');
}
