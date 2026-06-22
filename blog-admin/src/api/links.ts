import request from '@/utils/request'
import type { Link } from '@/types'

export function getLinks() {
  return request.get<any, Result<Link[]>>('/admin/links')
}

export function createLink(data: { name: string; url: string; description?: string; sort?: number; status?: number }) {
  return request.post<any, Result<number>>('/admin/links', data)
}

export function updateLink(id: number, data: { name?: string; url?: string; description?: string; sort?: number; status?: number }) {
  return request.put<any, Result<void>>(`/admin/links/${id}`, data)
}

export function deleteLink(id: number) {
  return request.delete<any, Result<void>>(`/admin/links/${id}`)
}
