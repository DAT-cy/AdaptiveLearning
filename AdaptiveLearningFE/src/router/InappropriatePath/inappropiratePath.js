import { loadView } from '@/utils/handleRouter';

export default [
  {
    path: '/404',
    name: 'NotFound',
    meta: {
      title: 'NotFound'
    },
    component: loadView('InappropriatePath', 'NotFound'),
  },
]