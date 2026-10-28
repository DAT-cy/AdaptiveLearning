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
  {
    path: '/403',
    name: 'Forbidden',
    meta: {
      title: 'Truy cập bị từ chối',
      layout: 'only-view'
    },
    component: loadView('InappropriatePath', '403'),
  },
]

