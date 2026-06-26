import { AppRouteRecord } from '@/types/router'
import { orderOpsRoutes } from './order-ops'
import { dashboardRoutes } from './dashboard'
import { systemRoutes } from './system'
import { resultRoutes } from './result'
import { exceptionRoutes } from './exception'
import { themeRoutes } from './theme'

/**
 * 导出所有模块化路由
 */
export const routeModules: AppRouteRecord[] = [
  orderOpsRoutes,
  dashboardRoutes,
  systemRoutes,
  resultRoutes,
  exceptionRoutes,
  themeRoutes
]
