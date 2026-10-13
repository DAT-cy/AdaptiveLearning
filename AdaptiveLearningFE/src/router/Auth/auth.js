import { loadView } from '@/utils/handleRouter';

export default [
  {
    path: '/login',
    name: 'Login',
    meta: {
      title: 'Login',
      layout: 'only-view'
    },
    component: loadView('Auth', 'Login'),
  },
]