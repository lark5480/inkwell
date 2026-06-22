import request from '@/utils/request'
import type { TagDTO } from '@/types'

export function getTags() {
  return request.get<any, Result<TagDTO[]>>('/admin/tags')
}

export function createTag(data: { name: string; slug: string }) {
  return request.post<any, Result<number>>('/admin/tags', data)
}

export function updateTag(id: number, data: { name?: string; slug?: string }) {
  return request.put<any, Result<void>>(`/admin/tags/${id}`, data)
}

export function deleteTag(id: number) {
  return request.delete<any, Result<void>>(`/admin/tags/${id}`)
}
