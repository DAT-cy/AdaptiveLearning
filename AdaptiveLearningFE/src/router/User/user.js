const userHome = () => import('@/features/User/pages/Home.vue')
const pricingPage = () => import('@/features/User/pages/Pricing.vue')
const paymentStatusPage = () => import('@/features/User/pages/PaymentStatus.vue')

const userRoutes = [
  { path: '/home', name: 'UserHome', view: 'dashboard', title: 'Trang chủ' },
  { path: '/plans', name: 'LearningPlans', view: 'plans', title: 'Lộ trình học' },
  { path: '/courses', name: 'LearningCourses', view: 'courses', title: 'Khóa học' },
  { path: '/tests', name: 'LearningTests', view: 'tests', title: 'Bài kiểm tra' },
  { path: '/history', name: 'LearningHistory', view: 'history', title: 'Lịch sử làm bài' },
  { path: '/progress', name: 'LearningProgress', view: 'progress', title: 'Thống kê tiến độ' },
  { path: '/tutor', name: 'LearningTutor', view: 'tutor', title: 'Trợ lý AI' },
  { path: '/settings', name: 'LearningSettings', view: 'settings', title: 'Cài đặt' },
  { path: '/pricing', name: 'LearningPricing', title: 'Nâng cấp Premium', component: pricingPage },
  { path: '/payment/success', name: 'PaymentSuccess', title: 'Trạng thái thanh toán', component: paymentStatusPage },
  { path: '/payment/cancel', name: 'PaymentCancel', title: 'Thanh toán đã hủy', component: paymentStatusPage },
]

export default userRoutes.map(({ path, name, view, title, component }) => ({
    path,
    name,
    meta: {
      title,
      view,
      layout: 'only-view',
      allowedRoles: ['ROLE_USER'],
    },
    component: component || userHome,
  }))
