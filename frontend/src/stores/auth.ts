import { defineStore } from 'pinia'

import { authApi } from '@/api/modules'
import { clearToken, getToken, setToken } from '@/api/http'
import type { LoginRequest, RegisterRequest, User } from '@/api/types'

const USER_KEY = 'currentUser'

function readStoredUser() {
  const raw = window.localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as User
  } catch {
    window.localStorage.removeItem(USER_KEY)
    return null
  }
}

interface AuthState {
  token: string
  user: User | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    user: readStoredUser(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    homePath: (state) => (state.user?.role === 'ADMIN' ? '/admin/dashboard' : '/user/spaces'),
  },
  actions: {
    saveSession(token: string, user: User) {
      this.token = token
      this.user = user
      setToken(token)
      window.localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    async login(payload: LoginRequest) {
      const result = await authApi.login(payload)
      this.saveSession(result.token, result.user)
      return result.user
    },
    register(payload: RegisterRequest) {
      return authApi.register(payload)
    },
    async loadProfile() {
      if (!this.token) {
        return null
      }
      const user = await authApi.me()
      this.user = user
      window.localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },
    logout() {
      this.token = ''
      this.user = null
      clearToken()
      window.localStorage.removeItem(USER_KEY)
    },
  },
})
