import axios, { type AxiosRequestConfig } from 'axios'

import type { ApiResponse } from './types'

const TOKEN_KEY = 'token'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearToken()
      window.localStorage.removeItem('currentUser')
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || error.message || '请求失败'
  }
  if (error instanceof Error) {
    return error.message
  }
  return '请求失败'
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<ApiResponse<T>>(config)
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '请求失败')
    }
    return response.data.data
  } catch (error) {
    throw new Error(apiErrorMessage(error))
  }
}

export default http
