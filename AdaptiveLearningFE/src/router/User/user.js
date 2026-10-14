const userHome = () => import('@/features/User/pages/Home.vue')

const userRoutes = [
  { path: '/home', name: 'UserHome', view: 'dashboard', title: 'Trang chủ' },
  { path: '/plans', name: 'LearningPlans', view: 'plans', title: 'Lộ trình học' },
  { path: '/courses', name: 'LearningCourses', view: 'courses', title: 'Khóa học' },
  { path: '/tests', name: 'LearningTests', view: 'tests', title: 'Bài kiểm tra' },
  { path: '/history', name: 'LearningHistory', view: 'history', title: 'Lịch sử làm bài' },
  { path: '/progress', name: 'LearningProgress', view: 'progress', title: 'Thống kê tiến độ' },
  { path: '/tutor', name: 'LearningTutor', view: 'tutor', title: 'Trợ lý AI' },
  { path: '/settings', name: 'LearningSettings', view: 'settings', title: 'Cài đặt' },
]

export default userRoutes.map(({ path, name, view, title }) => ({
  path,
  name,
  meta: {
    title,
    view,
    layout: 'only-view',
    allowedRoles: ['ROLE_USER'],
  },
  component: userHome,
}))
