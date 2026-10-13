import { loadView } from '@/utils/handleRouter';

export default [
  {
    path: '/home',
    name: 'Home',
    meta: {
      title: 'Trang chủ',
      layout: 'only-view'
    },
    component: () => import('@/features/Home/pages/Home.vue')
  },
]
