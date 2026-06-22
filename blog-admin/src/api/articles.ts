import request from '@/utils/request'
import type { Article, ArticleCreateRequest, ArticleUpdateRequest } from '@/types'

export interface ArticleQuery {
  page?: number
  pageSize?: number
  keyword?: string
  categoryId?: number
  status?: string
}

export function getArticles(params?: ArticleQuery) {
  return request.get<any, Result<PageDTO<Article>>>('/admin/articles', { params })
}

export function getArticle(id: number) {
  return request.get<any, Result<Article>>(`/admin/articles/${id}`)
}

export function createArticle(data: ArticleCreateRequest) {
  return request.post<any, Result<number>>('/admin/articles', data)
}

export function updateArticle(id: number, data: ArticleUpdateRequest) {
  return request.put<any, Result<void>>(`/admin/articles/${id}`, data)
}

export function deleteArticle(id: number) {
  return request.delete<any, Result<void>>(`/admin/articles/${id}`)
}

export function updateArticleStatus(id: number, status: string) {
  return request.put<any, Result<void>>(`/admin/articles/${id}/status`, { status })
}

export function uploadImage(formData: FormData) {
  return request.post<any, Result<{ url: string }>>('/admin/articles/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
