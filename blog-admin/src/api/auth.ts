import request from '@/utils/request'
import type { LoginResponse, UserInfo } from '@/types'

export function login(data: { username: string; password: string }) {
  return request.post<any, Result<LoginResponse>>('/admin/auth/login', data)
}

export function logout() {
  return request.post<any, Result<void>>('/admin/auth/logout')
}

export function getCurrentUser() {
  return request.get<any, Result<UserInfo>>('/admin/auth/me')
}
