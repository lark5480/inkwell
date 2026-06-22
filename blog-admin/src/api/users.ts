import request from '@/utils/request'
import type { User } from '@/types'

export function getUsers() {
  return request.get<any, Result<User[]>>('/admin/users')
}

export function getUser(id: number) {
  return request.get<any, Result<User>>(`/admin/users/${id}`)
}

export function updateUserStatus(id: number, status: number) {
  return request.put<any, Result<void>>(`/admin/users/${id}/status`, { status })
}

export function updateUserRole(id: number, role: string) {
  return request.put<any, Result<void>>(`/admin/users/${id}/role`, { role })
}
