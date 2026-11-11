const adminHome = () => import('@/features/Admin/pages/Home.vue')
const overlayDemo = () => import('@/features/Admin/pages/OverlayDemo.vue')
const reports = () => import('@/features/Admin/pages/Reports.vue')
const examImports = () => import('@/features/Admin/pages/ExamImports.vue')

export default [
  {
    path: '/admin/exam-imports',
    name: 'AdminExamImports',
    meta: { title: 'Nhập bộ đề', layout: 'only-view', allowedRoles: ['ROLE_ADMIN'] },
    component: examImports,
  },
  {
    path: '/teacher/review',
    name: 'TeacherExamReview',
    meta: { title: 'Duyệt bộ đề', layout: 'only-view', allowedRoles: ['ROLE_TEACHER'] },
    component: examImports,
  },
  {
    path: '/admin/reports',
    name: 'AdminReports',
    meta: { title: 'Báo cáo học tập', layout: 'only-view', allowedRoles: ['ROLE_ADMIN'] },
    component: reports,
  },
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
  {
    path: '/admin/:section(overview|accounts|billing|results|programs|exams|knowledge|question-bank|exam-sets)',
    name: 'AdminSection',
    meta: {
      title: 'Quản trị hệ thống',
      layout: 'only-view',
      allowedRoles: ['ROLE_ADMIN'],
    },
    component: adminHome,
  },
]
