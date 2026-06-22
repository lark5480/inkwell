import request from '@/utils/request'
import type { StatsOverview } from '@/types'

export function getOverview() {
  return request.get<any, Result<StatsOverview>>('/admin/stats/overview')
}
