import { openApiRequest } from './openapiClient'
import type {
  OrderOpsAgentPlan,
  OrderOpsApprovalDecisionParams,
  OrderOpsApprovalList,
  OrderOpsApprovalListQuery,
  OrderOpsApprovalTicket,
  OrderOpsAuditLogList,
  OrderOpsAuditLogQuery,
  OrderOpsCompensationTask,
  OrderOpsExecuteParams,
  OrderOpsExecutionResult,
  OrderOpsOrder,
  OrderOpsOrderList,
  OrderOpsOrderListQuery,
  OrderOpsPlanParams,
  OrderOpsToolList
} from './model/orderOpsModel'

/** Fetch teachable demo orders for Agent execution scenarios. */
export function fetchOrderOpsOrders(query?: OrderOpsOrderListQuery): Promise<OrderOpsOrderList> {
  return openApiRequest('listOrderOpsOrders', { query })
}

/** Fetch one order with timeline and operation capability flags. */
export function fetchOrderOpsOrderDetail(orderNo: string): Promise<OrderOpsOrder> {
  return openApiRequest('getOrderOpsOrderDetail', {
    pathParams: { orderNo }
  })
}

/** Fetch available Tool Calling definitions. */
export function fetchOrderOpsTools(): Promise<OrderOpsToolList> {
  return openApiRequest('listOrderOpsTools')
}

/** Ask the Agent to build a deterministic action plan. */
export function planOrderOpsAction(params: OrderOpsPlanParams): Promise<OrderOpsAgentPlan> {
  return openApiRequest('planOrderOpsAgentAction', {
    body: params
  })
}

/** Execute an Agent action with idempotency and optional human confirmation. */
export function executeOrderOpsAction(
  params: OrderOpsExecuteParams
): Promise<OrderOpsExecutionResult> {
  return openApiRequest('executeOrderOpsAgentAction', {
    body: params,
    showSuccessMessage: true
  })
}

/** Fetch pending or historical approval tickets. */
export function fetchOrderOpsApprovals(
  query?: OrderOpsApprovalListQuery
): Promise<OrderOpsApprovalList> {
  return openApiRequest('listOrderOpsApprovals', { query })
}

/** Confirm a high-risk Agent execution ticket. */
export function confirmOrderOpsApproval(
  approvalId: string,
  params: OrderOpsApprovalDecisionParams
): Promise<OrderOpsApprovalTicket> {
  return openApiRequest('confirmOrderOpsApproval', {
    pathParams: { approvalId },
    body: params,
    showSuccessMessage: true
  })
}

/** Reject a high-risk Agent execution ticket. */
export function rejectOrderOpsApproval(
  approvalId: string,
  params: OrderOpsApprovalDecisionParams
): Promise<OrderOpsApprovalTicket> {
  return openApiRequest('rejectOrderOpsApproval', {
    pathParams: { approvalId },
    body: params,
    showSuccessMessage: true
  })
}

/** Fetch append-only Agent execution audit logs. */
export function fetchOrderOpsAuditLogs(query?: OrderOpsAuditLogQuery): Promise<OrderOpsAuditLogList> {
  return openApiRequest('listOrderOpsAuditLogs', { query })
}

/** Retry a compensation task created by a failed execution stage. */
export function retryOrderOpsCompensation(
  compensationId: string,
  params: OrderOpsApprovalDecisionParams
): Promise<OrderOpsCompensationTask> {
  return openApiRequest('retryOrderOpsCompensation', {
    pathParams: { compensationId },
    body: params,
    showSuccessMessage: true
  })
}
