import { createRouter, createWebHashHistory } from 'vue-router'
import { session, checkSession } from './api'
import Landing from './views/Landing.vue'
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: Landing },
    { path: '/login', component: () => import('./views/Login.vue') },
    {
      path: '/admin',
      component: () => import('./views/Admin.vue'),
      meta: { role: 'admin' },
    },
    { path: '/screen', component: () => import('./views/Screen.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
router.beforeEach(async (to) => {
  if (!to.meta.role) return true
  if (!session.checked || !session.user) await checkSession()
  if (!session.user) return { path: '/login', query: { role: to.meta.role } }
  if (session.user.role !== 'admin') return '/login'
  return true
})
export default router
