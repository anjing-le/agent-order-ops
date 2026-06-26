/**
 * Order operations Agent API model types.
 *
 * @module api/model/orderOpsModel
 */

import type {
  OpenApiOperationData,
  OpenApiOperationQuery,
  OpenApiOperationRequest
} from '@/contracts/openapi/operations'

/** Supported executable order operation actions. */
export type OrderOpsActionType = OpenApiOperationRequest<'planOrderOpsAgentAction'>['actionType']

/** Order list query params. */
export type OrderOpsOrderListQuery = OpenApiOperationQuery<'listOrderOpsOrders'>

/** Approval list query params. */
export type OrderOpsApprovalListQuery = OpenApiOperationQuery<'listOrderOpsApprovals'>

/** Audit log query params. */
export type OrderOpsAuditLogQuery = OpenApiOperationQuery<'listOrderOpsAuditLogs'>

/** Order status values. */
export type OrderOpsOrderStatus = NonNullable<OrderOpsOrderListQuery['orderStatus']>

/** Approval status values. */
export type OrderOpsApprovalStatus = NonNullable<OrderOpsApprovalListQuery['status']>

/** Agent plan request params. */
export type OrderOpsPlanParams = OpenApiOperationRequest<'planOrderOpsAgentAction'>

/** Agent execution request params. */
export type OrderOpsExecuteParams = OpenApiOperationRequest<'executeOrderOpsAgentAction'>

/** Approval decision request params. */
export type OrderOpsApprovalDecisionParams = OpenApiOperationRequest<'confirmOrderOpsApproval'>

/** Order detail view. */
export type OrderOpsOrder = OpenApiOperationData<'getOrderOpsOrderDetail'>

/** Order list view. */
export type OrderOpsOrderList = OpenApiOperationData<'listOrderOpsOrders'>

/** Tool registry list view. */
export type OrderOpsToolList = OpenApiOperationData<'listOrderOpsTools'>

/** Tool registry item view. */
export type OrderOpsToolDefinition = OrderOpsToolList[number]

/** Agent plan view. */
export type OrderOpsAgentPlan = OpenApiOperationData<'planOrderOpsAgentAction'>

/** Tool call view. */
export type OrderOpsToolCall = NonNullable<OrderOpsAgentPlan['toolCalls']>[number]

/** Agent execution result view. */
export type OrderOpsExecutionResult = OpenApiOperationData<'executeOrderOpsAgentAction'>

/** Execution status values. */
export type OrderOpsExecutionStatus = NonNullable<OrderOpsExecutionResult['status']>

/** Approval ticket view. */
export type OrderOpsApprovalTicket = OpenApiOperationData<'confirmOrderOpsApproval'>

/** Approval ticket list view. */
export type OrderOpsApprovalList = OpenApiOperationData<'listOrderOpsApprovals'>

/** Audit log list view. */
export type OrderOpsAuditLogList = OpenApiOperationData<'listOrderOpsAuditLogs'>

/** Audit log item view. */
export type OrderOpsAuditLog = OrderOpsAuditLogList[number]

/** Compensation task view. */
export type OrderOpsCompensationTask = OpenApiOperationData<'retryOrderOpsCompensation'>
