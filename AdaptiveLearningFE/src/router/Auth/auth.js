import { loadView } from '@/utils/handleRouter';

export default [
  {
    path: '/login',
    name: 'Login',
    meta: {
      title: 'Đăng nhập',
      layout: 'only-view'
    },
    component: loadView('Auth', 'Login'),
  },
  {
    path: '/register',
    name: 'Register',
    meta: { title: 'Tạo tài khoản', layout: 'only-view' },
    component: loadView('Auth', 'Register'),
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    meta: { title: 'Xác minh email', layout: 'only-view' },
    component: loadView('Auth', 'VerifyEmail'),
  },
]
