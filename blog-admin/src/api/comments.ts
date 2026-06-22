import request from '@/utils/request'
import type { Comment } from '@/types'

export interface CommentQuery {
  page?: number
  pageSize?: number
  status?: string
}

export function getComments(params?: CommentQuery) {
  return request.get<any, Result<PageDTO<Comment>>>('/admin/comments', { params })
}

export function updateCommentStatus(id: number, status: string) {
  return request.put<any, Result<void>>(`/admin/comments/${id}/status`, { status })
}

export function deleteComment(id: number) {
  return request.delete<any, Result<void>>(`/admin/comments/${id}`)
}
