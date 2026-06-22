import request from '@/utils/request'
import type { Category } from '@/types'

export function getCategories() {
  return request.get<any, Result<Category[]>>('/admin/categories')
}

export function createCategory(data: { name: string; slug: string; description?: string; sort?: number }) {
  return request.post<any, Result<number>>('/admin/categories', data)
}

export function updateCategory(id: number, data: { name?: string; slug?: string; description?: string; sort?: number }) {
  return request.put<any, Result<void>>(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete<any, Result<void>>(`/admin/categories/${id}`)
}
