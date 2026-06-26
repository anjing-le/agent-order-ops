import { AppRouteRecord } from '@/types/router'

export const orderOpsRoutes: AppRouteRecord = {
  name: 'OrderOps',
  path: '/order-ops',
  component: '/index/index',
  meta: {
    title: 'menus.orderOps.title',
    icon: 'ri:robot-2-line',
    roles: ['R_SUPER', 'R_ADMIN', 'R_GUEST']
  },
  children: [
    {
      path: 'console',
      name: 'OrderOpsConsole',
      component: '/order-ops/console',
      meta: {
        title: 'menus.orderOps.console',
        keepAlive: false,
        fixedTab: true
      }
    }
  ]
}
