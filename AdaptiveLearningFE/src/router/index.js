import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'

import auth from './Auth/auth'
import userRoutes from './User/user'
import adminRoutes from './Admin/admin'
import inappropriatePath from './InappropriatePath/inappropiratePath'
import { settings } from '@/config/settings'

const { routerMode } = settings

export const constantRoutes = [
  { path: '/', redirect: '/home' },
  ...auth,
  ...userRoutes,
  ...adminRoutes,
  ...inappropriatePath,
  {
    path: "/:pathMatch(.*)*",
    redirect: { name: 'NotFound' }
  },
]


const router = createRouter({
  history: routerMode === 'hash' ? createWebHashHistory(import.meta.env.VITE_BASE_URL) : createWebHistory(import.meta.env.VITE_BASE_URL),
  routes: constantRoutes,
  /**
      * Cuộn đến một vị trí cụ thể trên trang với thao tác mượt mà.
      *
      * @param {any} to - Vị trí đích cần cuộn tới.
      * @param {any} from - Vị trí hiện tại để cuộn từ đó.
      * @param {any} savePosition - Vị trí đã lưu để cuộn tới.
      * @return {Promise} Một lời hứa sẽ được giải quyết khi quá trình cuộn hoàn tất.
      */
  scrollBehavior(to, from, savedPosition) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (to.hash) {
          resolve({ selector: to.hash });
        } else if (savedPosition) {
          resolve(savedPosition);
        } else {
          const appElement = document.getElementById("app");
          resolve(appElement.scrollIntoView({ behavior: "smooth" }));
        }
      }, 500);
    });
  }
})

export default router
