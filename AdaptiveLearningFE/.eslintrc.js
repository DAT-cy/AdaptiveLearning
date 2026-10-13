module.exports = {
  // Cấu hình để cho biết rằng nó là một tệp cấu hình ESLint ở gốc (root)
  root: true,
  // Cấu hình môi trường để cho biết rằng nó đang chạy trong môi trường Node
  env: {
    node: true,
  },
  // Cấu hình mở rộng cho tệp cấu hình ESLint. Các tệp cấu hình này được áp dụng theo thứ tự bắt đầu từ trên xuống dưới
  extends: ['plugin:vue/vue3-essential', 'eslint:recommended', '@vue/prettier'],
  // Cấu hình parserOptions để cho biết rằng parser sử dụng là babel-eslint
  parserOptions: {
    parser: 'babel-eslint',
  },
  // Các quy tắc cấu hình
  rules: {
    // Hiển thị cảnh báo khi sử dụng console trong môi trường production
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    // Hiển thị cảnh báo khi sử dụng debugger trong môi trường production
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
  },
}
