/* eslint-disable */
// Generated from OpenAPI JSON. Do not edit manually.
// Run: node scripts/generate-openapi-frontend-types.js <openapi-json-file>

export type JsonObject = Record<string, unknown>

export interface AgentExecuteRequest {
  actionType: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  confirmRisk?: boolean
  couponAmount?: number
  idempotencyKey: string
  newAddress?: string
  operatorId?: string
  operatorName?: string
  orderNo: string
  reason?: string
  refundAmount?: number
  simulateFailureStage?: string
}

export interface AgentPlanRequest {
  actionType: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  couponAmount?: number
  newAddress?: string
  operatorId?: string
  operatorName?: string
  orderNo: string
  reason?: string
  refundAmount?: number
}

export interface AgentPlanView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  approvalReason?: string
  compensationPlan?: string
  humanBoundary?: string
  orderNo?: string
  planId?: string
  requiresApproval?: boolean
  riskLevel?: "LOW" | "MEDIUM" | "HIGH"
  summary?: string
  toolCalls?: ToolCallView[]
}

export interface APIResponseAgentPlanView {
  code?: string
  data?: AgentPlanView
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseApprovalTicketView {
  code?: string
  data?: ApprovalTicketView
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseAuthTokenResponse {
  code?: string
  data?: AuthTokenResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseCompensationTaskView {
  code?: string
  data?: CompensationTaskView
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseCurrentUserResponse {
  code?: string
  data?: CurrentUserResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseExecutionResultView {
  code?: string
  data?: ExecutionResultView
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseListApprovalTicketView {
  code?: string
  data?: ApprovalTicketView[]
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseListAuditLogView {
  code?: string
  data?: AuditLogView[]
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseListOrderView {
  code?: string
  data?: OrderView[]
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseListToolDefinitionView {
  code?: string
  data?: ToolDefinitionView[]
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseMapStringObject {
  code?: string
  data?: Record<string, unknown>
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseMiddlewareStatusReport {
  code?: string
  data?: MiddlewareStatusReport
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseOrderView {
  code?: string
  data?: OrderView
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseString {
  code?: string
  data?: string
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseVoid {
  code?: string
  data?: unknown
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface ApprovalDecisionRequest {
  comment?: string
  operatorId: string
  operatorName?: string
}

export interface ApprovalTicketView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  approvalId?: string
  decidedAt?: string
  decidedBy?: string
  decisionComment?: string
  orderNo?: string
  plan?: AgentPlanView
  reason?: string
  requestedAt?: string
  requestedBy?: string
  result?: ExecutionResultView
  riskLevel?: "LOW" | "MEDIUM" | "HIGH"
  status?: "PENDING" | "APPROVED" | "REJECTED"
}

export interface AuditLogView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  auditId?: string
  createdAt?: string
  detail?: string
  eventType?: "ORDER_QUERIED" | "PLAN_CREATED" | "IDEMPOTENCY_REPLAYED" | "APPROVAL_CREATED" | "APPROVAL_CONFIRMED" | "APPROVAL_REJECTED" | "TOOL_CALL_STARTED" | "TOOL_CALL_SUCCEEDED" | "TOOL_CALL_FAILED" | "EXECUTION_SUCCEEDED" | "EXECUTION_FAILED" | "COMPENSATION_CREATED" | "COMPENSATION_RETRIED" | "COMPENSATION_SUCCEEDED" | "COMPENSATION_FAILED"
  message?: string
  operatorId?: string
  orderNo?: string
}

/**
 * Authentication token payload
 */
export interface AuthTokenResponse {
  /**
   * Access token used in Authorization header
   */
  accessToken: string
  /**
   * Access token lifetime in seconds
   */
  expiresIn: number
  /**
   * Refresh token used to renew access token
   */
  refreshToken: string
  /**
   * Token type
   */
  tokenType: string
}

export interface CompensationTaskView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  compensationAction?: string
  compensationId?: string
  createdAt?: string
  failedStep?: string
  lastError?: string
  orderNo?: string
  retryCount?: number
  status?: "PENDING" | "SUCCEEDED" | "FAILED"
  updatedAt?: string
}

/**
 * Current authenticated user payload
 */
export interface CurrentUserResponse {
  /**
   * Avatar URL
   */
  avatar?: string
  /**
   * User creation time in ISO-8601 UTC format
   */
  createTime?: string
  /**
   * User email
   */
  email?: string
  /**
   * Display nickname
   */
  nickName?: string
  /**
   * Permission codes
   */
  permissions?: string[]
  /**
   * Role codes
   */
  roles: string[]
  /**
   * User id
   */
  userId: number
  /**
   * Login username
   */
  userName: string
}

export interface ExecutionResultView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  approvalId?: string
  compensationTask?: CompensationTaskView
  executionId?: string
  idempotencyKey?: string
  orderNo?: string
  replay?: boolean
  status?: "WAITING_APPROVAL" | "SUCCESS" | "FAILED"
  summary?: string
  toolCalls?: ToolCallView[]
}

/**
 * Login request
 */
export interface LoginRequest {
  /**
   * Captcha code when enabled
   */
  captcha?: string
  /**
   * Password
   */
  password: string
  /**
   * Whether to keep the session longer
   */
  rememberMe?: boolean
  /**
   * Username or email
   */
  username: string
}

export interface MiddlewareInfo {
  details?: string
  enabled?: boolean
  name?: string
  status?: "disabled" | "configured" | "ready" | "degraded"
  statusCode?: string
  statusDescription?: string
  version?: string
}

export interface MiddlewareStatusReport {
  features?: MiddlewareInfo[]
  status?: "disabled" | "configured" | "ready" | "degraded"
  statusCode?: string
  statusDescription?: string
  summary?: MiddlewareSummary
}

export interface MiddlewareSummary {
  byStatus?: Record<string, number>
  enabled?: number
  total?: number
}

export interface OrderTimelineItem {
  detail?: string
  time?: string
  title?: string
}

export interface OrderView {
  address?: string
  canChangeAddress?: boolean
  canCompensate?: boolean
  canIntercept?: boolean
  canRefund?: boolean
  couponIssuedAmount?: number
  customerName?: string
  orderNo?: string
  orderStatus?: "CREATED" | "PAID" | "FULFILLING" | "SHIPPED" | "DELIVERED" | "CLOSED" | "CANCELLED"
  paidAmount?: number
  paymentStatus?: "UNPAID" | "PAID" | "REFUNDING" | "PARTIALLY_REFUNDED" | "REFUNDED"
  phoneMasked?: string
  refundableAmount?: number
  refundedAmount?: number
  riskTags?: string[]
  shipmentStatus?: "NOT_CREATED" | "PENDING" | "ALLOCATED" | "INTERCEPTED" | "IN_TRANSIT" | "DELIVERED" | "RETURNING"
  timeline?: OrderTimelineItem[]
}

/**
 * Refresh token request
 */
export interface RefreshTokenRequest {
  /**
   * Refresh token returned by login
   */
  refreshToken: string
}

export interface ToolCallView {
  finishedAt?: string
  inputSummary?: string
  outputSummary?: string
  startedAt?: string
  status?: "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "SKIPPED"
  stepName?: string
  toolName?: string
}

export interface ToolDefinitionView {
  actionType?: "QUERY_ORDER" | "INTERCEPT_SHIPMENT" | "CHANGE_ADDRESS" | "APPLY_REFUND" | "ISSUE_COUPON"
  description?: string
  idempotencyHint?: string
  requiresApprovalHint?: string
  riskLevel?: "LOW" | "MEDIUM" | "HIGH"
  title?: string
  toolName?: string
}

export interface OpenApiSchemas {
  AgentExecuteRequest: AgentExecuteRequest
  AgentPlanRequest: AgentPlanRequest
  AgentPlanView: AgentPlanView
  APIResponseAgentPlanView: APIResponseAgentPlanView
  APIResponseApprovalTicketView: APIResponseApprovalTicketView
  APIResponseAuthTokenResponse: APIResponseAuthTokenResponse
  APIResponseCompensationTaskView: APIResponseCompensationTaskView
  APIResponseCurrentUserResponse: APIResponseCurrentUserResponse
  APIResponseExecutionResultView: APIResponseExecutionResultView
  APIResponseListApprovalTicketView: APIResponseListApprovalTicketView
  APIResponseListAuditLogView: APIResponseListAuditLogView
  APIResponseListOrderView: APIResponseListOrderView
  APIResponseListToolDefinitionView: APIResponseListToolDefinitionView
  APIResponseMapStringObject: APIResponseMapStringObject
  APIResponseMiddlewareStatusReport: APIResponseMiddlewareStatusReport
  APIResponseOrderView: APIResponseOrderView
  APIResponseString: APIResponseString
  APIResponseVoid: APIResponseVoid
  ApprovalDecisionRequest: ApprovalDecisionRequest
  ApprovalTicketView: ApprovalTicketView
  AuditLogView: AuditLogView
  AuthTokenResponse: AuthTokenResponse
  CompensationTaskView: CompensationTaskView
  CurrentUserResponse: CurrentUserResponse
  ExecutionResultView: ExecutionResultView
  LoginRequest: LoginRequest
  MiddlewareInfo: MiddlewareInfo
  MiddlewareStatusReport: MiddlewareStatusReport
  MiddlewareSummary: MiddlewareSummary
  OrderTimelineItem: OrderTimelineItem
  OrderView: OrderView
  RefreshTokenRequest: RefreshTokenRequest
  ToolCallView: ToolCallView
  ToolDefinitionView: ToolDefinitionView
}
