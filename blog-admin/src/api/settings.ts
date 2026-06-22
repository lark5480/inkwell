import request from '@/utils/request'
import type { Setting } from '@/types'

export function getSettings() {
  return request.get<any, Result<Setting[]>>('/admin/settings')
}

export function updateSettings(data: Record<string, string>) {
  return request.put<any, Result<void>>('/admin/settings', data)
}
