const adminHome = () => import('@/features/Admin/pages/Home.vue')

export default [
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
