import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../api'

export interface User {
  id: number
  username: string
  nickname: string
  totalFocusSeconds: number
  role?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function fetchMe() {
    try {
      user.value = await api.get<User>('/api/user/me')
    } catch {
      user.value = null
    }
  }

  async function login(username: string, password: string) {
    user.value = await api.post<User>('/api/user/login', { username, password })
  }

  async function register(username: string, password: string, nickname?: string) {
    await api.post('/api/user/register', { username, password, nickname })
    await login(username, password)
  }

  async function logout() {
    await api.post('/api/user/logout')
    user.value = null
  }

  return { user, isAdmin, fetchMe, login, register, logout }
})
