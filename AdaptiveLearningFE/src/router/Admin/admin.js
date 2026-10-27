const adminHome = () => import('@/features/Admin/pages/Home.vue')
const overlayDemo = () => import('@/features/Admin/pages/OverlayDemo.vue')

export default [
  {
    path: '/dev/overlays',
    name: 'OverlayDemo',
    meta: {
      title: 'Thư viện lớp phủ',
      layout: 'only-view',
      allowedRoles: ['ROLE_ADMIN'],
    },
    component: overlayDemo,
  },
  {
    path: '/admin',
    name: 'AdminHome',
    meta: {
      title: 'Quản trị hệ thống',
      layout: 'only-view',
      allowedRoles: ['ROLE_ADMIN'],
    },
    component: adminHome,
  },
]
