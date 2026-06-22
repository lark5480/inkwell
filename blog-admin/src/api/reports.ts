import request from '@/utils/request'

export interface CommentReport {
  id: number
  commentId: number
  reporterId: number
  reason: string
  status: string
  createTime: string
  resolveTime: string | null
}

export function getReports(params?: { page?: number; pageSize?: number; status?: string }) {
  return request.get<any, Result<PageDTO<CommentReport>>>('/admin/reports', { params })
}

export function updateReportStatus(id: number, status: string) {
  return request.put<any, Result<void>>(`/admin/reports/${id}/status`, { status })
}
