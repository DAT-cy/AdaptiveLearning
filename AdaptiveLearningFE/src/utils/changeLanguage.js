import { getCurrentInstance } from 'vue'

/**
 ** Hàm để thay đổi ngôn ngữ hiện tại của ứng dụng.
 *
 * @param {string} locale - Mã locale để đặt là ngôn ngữ mới.
 * @return {void} Hàm này không trả về gì.
 */
export function changeLanguage(locale) {
  //* Lấy Vue instance hiện tại.
  const app = getCurrentInstance();

  //* Lấy instance i18n từ các thuộc tính toàn cầu của Vue app.
  const i18n = app?.appContext?.app?.config?.globalProperties?.$i18n;

  //* Kiểm tra xem locale được cung cấp có có sẵn trong i18n instance không.
  //* Nếu có, đặt nó là ngôn ngữ mới.
  if (i18n?.availableLocales.includes(locale)) {
    //* Đặt locale mới.
    i18n.locale = locale;
  }
}
