import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/lobby', name: 'lobby', component: () => import('../views/Lobby.vue'), meta: { auth: true } },
  { path: '/room/:id', name: 'room', component: () => import('../views/Room.vue'), meta: { auth: true }, props: true },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  { path: '/rank', name: 'rank', component: () => import('../views/Rank.vue'), meta: { auth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/Admin.vue'), meta: { auth: true, admin: true } },
  { path: '/', redirect: '/lobby' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.auth && !userStore.user) {
    return { name: 'login' }
  }
  if (to.meta.admin && !userStore.isAdmin) {
    return { name: 'lobby' }
  }
})

export default router
