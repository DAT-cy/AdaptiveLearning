import { loadView } from '@/utils/handleRouter';

export default [
  {
    path: '/404',
    name: 'NotFound',
    meta: {
      title: 'Không tìm thấy trang',
      layout: 'only-view'
    },
    component: loadView('InappropriatePath', '404'),
  },
]
