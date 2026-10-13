/**
 * Hàm tạo ra một UUID mới bằng cách sử dụng thời gian hiện tại và thuật toán xâu chuỗi.
 *
 * @returns {string} - Một chuỗi UUID mới.
 * @example
 * genernateUUID() // => "123e4567-e89b-44dc-9aa6-1234567890ab"
 */
export const genernateUUID = () => {
  let dt = new Date().getTime()
  const uuid = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (dt + Math.random() * 16) % 16 | 0;
    dt = Math.floor(dt / 16);
    return (c === "x" ? r : (r & 0x3 | 0x8)).toString(16);
  })
  return uuid
}

