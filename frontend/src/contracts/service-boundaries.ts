/* eslint-disable */
// Generated from contracts/service-boundaries.json. Do not edit manually.
// Run: node scripts/generate-service-boundaries-frontend.js

export const SERVICE_BOUNDARY_CONTRACT = {
  "schemaVersion": 1,
  "applicationId": "agent-order-ops",
  "apiPrefix": "/api",
  "boundaries": [
    {
      "id": "auth",
      "name": "Authentication",
      "kind": "runtime",
      "owner": "infra-auth",
      "currentHost": "agent-order-ops",
      "basePath": "/api/auth",
      "apiConstantsClass": "Auth",
      "apiPathsKey": "auth",
      "controller": "backend/src/main/java/com/anjing/controller/AuthController.java",
      "openapi": true,
      "copyAction": "replace with real auth center or database-backed auth",
      "routes": [
        {
          "name": "login",
          "backendConstant": "LOGIN_FULL",
          "frontendKey": "login",
          "path": "/api/auth/login",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "logout",
          "backendConstant": "LOGOUT_FULL",
          "frontendKey": "logout",
          "path": "/api/auth/logout",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "currentUser",
          "backendConstant": "ME_FULL",
          "frontendKey": "me",
          "path": "/api/auth/me",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "refreshToken",
          "backendConstant": "REFRESH_FULL",
          "frontendKey": "refresh",
          "path": "/api/auth/refresh",
          "methods": [
            "POST"
          ]
        }
      ]
    },
    {
      "id": "test",
      "name": "Project Test",
      "kind": "sample",
      "owner": "agent-order-ops",
      "currentHost": "agent-order-ops",
      "basePath": "/api/test",
      "apiConstantsClass": "Test",
      "apiPathsKey": "test",
      "controller": "backend/src/main/java/com/anjing/controller/TestController.java",
      "openapi": true,
      "copyAction": "delete or replace after the copied project has its own health and sample strategy",
      "routes": [
        {
          "name": "health",
          "backendConstant": "HEALTH_FULL",
          "frontendKey": "health",
          "path": "/api/test/health",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "features",
          "backendConstant": "FEATURES_FULL",
          "frontendKey": "features",
          "path": "/api/test/features",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "ping",
          "backendConstant": "PING_FULL",
          "frontendKey": "ping",
          "path": "/api/test/ping",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "bizException",
          "backendConstant": "EXCEPTION_BIZ_FULL",
          "frontendKey": "bizException",
          "path": "/api/test/exception/biz",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "systemException",
          "backendConstant": "EXCEPTION_SYSTEM_FULL",
          "frontendKey": "systemException",
          "path": "/api/test/exception/system",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "items",
          "backendConstant": "ITEMS_FULL",
          "frontendKey": "items",
          "path": "/api/test/items",
          "methods": [
            "GET",
            "POST"
          ]
        },
        {
          "name": "itemDetail",
          "backendConstant": "ITEM_DETAIL_FULL",
          "frontendKey": "itemDetail",
          "path": "/api/test/items/{id}",
          "methods": [
            "GET",
            "PUT",
            "DELETE"
          ]
        }
      ]
    },
    {
      "id": "order-ops",
      "name": "Order Operations Agent",
      "kind": "runtime",
      "owner": "agent-order-ops",
      "currentHost": "agent-order-ops",
      "basePath": "/api/order-ops",
      "apiConstantsClass": "OrderOps",
      "apiPathsKey": "orderOps",
      "controller": "backend/src/main/java/com/anjing/controller/OrderOpsController.java",
      "openapi": true,
      "copyAction": "core runtime module for order operations agent teaching scenarios",
      "routes": [
        {
          "name": "orders",
          "backendConstant": "ORDERS_FULL",
          "frontendKey": "orders",
          "path": "/api/order-ops/orders",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "orderDetail",
          "backendConstant": "ORDER_DETAIL_FULL",
          "frontendKey": "orderDetail",
          "path": "/api/order-ops/orders/{orderNo}",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "tools",
          "backendConstant": "TOOLS_FULL",
          "frontendKey": "tools",
          "path": "/api/order-ops/tools",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "plan",
          "backendConstant": "PLAN_FULL",
          "frontendKey": "plan",
          "path": "/api/order-ops/agent/plan",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "execute",
          "backendConstant": "EXECUTE_FULL",
          "frontendKey": "execute",
          "path": "/api/order-ops/agent/execute",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "approvals",
          "backendConstant": "APPROVALS_FULL",
          "frontendKey": "approvals",
          "path": "/api/order-ops/approvals",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "approvalConfirm",
          "backendConstant": "APPROVAL_CONFIRM_FULL",
          "frontendKey": "approvalConfirm",
          "path": "/api/order-ops/approvals/{approvalId}/confirm",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "approvalReject",
          "backendConstant": "APPROVAL_REJECT_FULL",
          "frontendKey": "approvalReject",
          "path": "/api/order-ops/approvals/{approvalId}/reject",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "auditLogs",
          "backendConstant": "AUDIT_LOGS_FULL",
          "frontendKey": "auditLogs",
          "path": "/api/order-ops/audit-logs",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "compensationRetry",
          "backendConstant": "COMPENSATION_RETRY_FULL",
          "frontendKey": "compensationRetry",
          "path": "/api/order-ops/compensations/{compensationId}/retry",
          "methods": [
            "POST"
          ]
        }
      ]
    },
    {
      "id": "common",
      "name": "Common Platform",
      "kind": "reserved-runtime",
      "owner": "infra-common",
      "currentHost": "agent-order-ops",
      "basePath": "/api/common",
      "apiConstantsClass": "Common",
      "apiPathsKey": "common",
      "openapi": false,
      "copyAction": "keep only endpoints implemented by the copied project",
      "routes": [
        {
          "name": "upload",
          "backendConstant": "UPLOAD_FILE_FULL",
          "frontendKey": "upload",
          "path": "/api/common/upload",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "uploadImage",
          "backendConstant": "UPLOAD_IMAGE_FULL",
          "frontendKey": "uploadImage",
          "path": "/api/common/upload/image",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "uploadWangEditor",
          "backendConstant": "UPLOAD_WANG_EDITOR_FULL",
          "frontendKey": "uploadWangEditor",
          "path": "/api/common/upload/wangeditor",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "download",
          "backendConstant": "DOWNLOAD_FILE_FULL",
          "frontendKey": "download",
          "path": "/api/common/download/{fileId}",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "deleteFile",
          "backendConstant": "DELETE_FILE_FULL",
          "frontendKey": "deleteFile",
          "path": "/api/common/files/{fileId}",
          "methods": [
            "DELETE"
          ]
        }
      ]
    },
    {
      "id": "user",
      "name": "User Management",
      "kind": "reserved",
      "owner": "infra-auth",
      "currentHost": "future-service",
      "basePath": "/api/users",
      "apiConstantsClass": "User",
      "openapi": false,
      "copyAction": "use when the copied project implements real user management",
      "routes": []
    },
    {
      "id": "admin",
      "name": "Admin Operations",
      "kind": "reserved",
      "owner": "infra-admin",
      "currentHost": "future-service",
      "basePath": "/api/admin",
      "apiConstantsClass": "Admin",
      "openapi": false,
      "copyAction": "use for operations dashboards, logs, and platform admin APIs",
      "routes": []
    },
    {
      "id": "integration",
      "name": "External Integration",
      "kind": "reserved",
      "owner": "infra-integration",
      "currentHost": "future-service",
      "basePath": "/api/integration",
      "apiConstantsClass": "Integration",
      "openapi": false,
      "copyAction": "use for OSS, payment, notification, LLM provider, and other external adapters",
      "routes": []
    }
  ]
} as const

export const APPLICATION_ID = SERVICE_BOUNDARY_CONTRACT.applicationId
export const SERVICE_BOUNDARY_BASE_PATHS = {
  "admin": "/api/admin",
  "auth": "/api/auth",
  "common": "/api/common",
  "integration": "/api/integration",
  "orderOps": "/api/order-ops",
  "test": "/api/test",
  "user": "/api/users"
} as const
export const SERVICE_BOUNDARY_ROUTE_PATHS = {
  "auth": {
    "login": "/api/auth/login",
    "logout": "/api/auth/logout",
    "me": "/api/auth/me",
    "refresh": "/api/auth/refresh"
  },
  "common": {
    "deleteFile": "/api/common/files/{fileId}",
    "download": "/api/common/download/{fileId}",
    "upload": "/api/common/upload",
    "uploadImage": "/api/common/upload/image",
    "uploadWangEditor": "/api/common/upload/wangeditor"
  },
  "orderOps": {
    "approvalConfirm": "/api/order-ops/approvals/{approvalId}/confirm",
    "approvalReject": "/api/order-ops/approvals/{approvalId}/reject",
    "approvals": "/api/order-ops/approvals",
    "auditLogs": "/api/order-ops/audit-logs",
    "compensationRetry": "/api/order-ops/compensations/{compensationId}/retry",
    "execute": "/api/order-ops/agent/execute",
    "orderDetail": "/api/order-ops/orders/{orderNo}",
    "orders": "/api/order-ops/orders",
    "plan": "/api/order-ops/agent/plan",
    "tools": "/api/order-ops/tools"
  },
  "test": {
    "bizException": "/api/test/exception/biz",
    "features": "/api/test/features",
    "health": "/api/test/health",
    "itemDetail": "/api/test/items/{id}",
    "items": "/api/test/items",
    "ping": "/api/test/ping",
    "systemException": "/api/test/exception/system"
  }
} as const

export type ServiceBoundaryContract = typeof SERVICE_BOUNDARY_CONTRACT
export type ServiceBoundaryId = ServiceBoundaryContract['boundaries'][number]['id']
export type ServiceBoundaryPathKey = keyof typeof SERVICE_BOUNDARY_BASE_PATHS
