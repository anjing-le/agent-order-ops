# Order Ops Agent Guide

本文是 `agent-order-ops` 的教学主线。它不把 Agent 描述成一个聊天入口，而是把 Agent 放在订单运营执行场景中，展示一次业务动作从“意图”到“计划、审批、执行、审计、补偿”的完整闭环。

## 学习目标

完成本项目演示后，学习者应该理解：

- Tool Calling 需要工具名、输入、输出、副作用、风险等级和失败补偿策略。
- 业务动作编排不是线性调用接口，而是要根据订单状态、履约状态、支付状态和风险标签决定下一步。
- 人工确认边界必须显式存在，高风险副作用动作不能只靠模型自觉。
- 幂等 key 是执行型 Agent 的基础设施，重复请求应重放结果，冲突请求应被拒绝。
- 审计日志要记录计划、工具调用、审批、失败和补偿，方便复盘和追责。
- 失败补偿不是“自动修好一切”，而是给人工或系统重试提供稳定入口。

## 业务模块地图

| 模块 | 后端动作 | 前端入口 | 教学点 |
|------|----------|----------|--------|
| 订单查询 | 读取订单上下文和时间线 | 订单池、订单详情 | 执行前先看事实，不直接动业务 |
| 发货拦截 | 调用履约拦截工具 | 动作编排区选择“发货拦截” | 高风险动作进入审批 |
| 地址修改 | 判断履约状态后修改收货地址 | 动作编排区填写新地址 | 状态约束和人工确认边界 |
| 退款申请 | 校验支付状态与金额 | 动作编排区填写退款金额 | 金额和支付状态校验 |
| 优惠补偿 | 发放优惠券补偿 | 动作编排区填写优惠券金额 | 低风险直执和幂等重放 |
| 审批确认 | 通过或拒绝审批单 | 右侧“审批确认”页签 | 人审后继续执行或终止 |
| 执行审计 | 写入审计日志和补偿任务 | 右侧“执行审计 / 失败补偿”页签 | 可追踪、可恢复 |

## 推荐演示路径

1. 启动后端和前端，登录游客账号，进入 `/order-ops/console`。
2. 在订单池选择一笔订单，点击“生成计划”，观察计划摘要和工具调用序列。
3. 选择“优惠补偿”，填写金额与原因，执行后保留同一个 idempotency key 再执行一次，观察幂等重放。
4. 选择“发货拦截”或“退款申请”，开启人工确认，执行后查看右侧待审批单。
5. 在审批确认页签点击通过，观察执行结果、订单状态和审计日志变化。
6. 开启失败注入再执行一次动作，查看失败补偿任务，然后点击重试。

## 工具注册表

| 工具 | 用途 | 副作用 | 审批边界 |
|------|------|--------|----------|
| `query_order_context` | 读取订单、支付、履约、风险和时间线 | 无 | 不需要审批 |
| `intercept_shipment` | 拦截已进入履约流程但未签收的包裹 | 有 | 通常需要人工确认 |
| `change_address` | 修改未完成订单的收货地址 | 有 | 高风险或已履约订单需要审批 |
| `apply_refund` | 创建退款申请并更新订单售后状态 | 有 | 金额相关动作需要审批 |
| `issue_coupon` | 发放优惠券补偿 | 有 | 低风险可直执，高金额可扩展审批 |
| `write_operation_audit` | 记录计划、执行、审批和补偿日志 | 有 | 不需要审批，但必须执行 |

## API 地图

| 能力 | Method | Path | operationId |
|------|--------|------|-------------|
| 订单列表 | GET | `/api/order-ops/orders` | `listOrderOpsOrders` |
| 订单详情 | GET | `/api/order-ops/orders/{orderNo}` | `getOrderOpsOrderDetail` |
| 工具列表 | GET | `/api/order-ops/tools` | `listOrderOpsTools` |
| 生成计划 | POST | `/api/order-ops/agent/plan` | `planOrderOpsAgentAction` |
| 执行动作 | POST | `/api/order-ops/agent/execute` | `executeOrderOpsAgentAction` |
| 审批列表 | GET | `/api/order-ops/approvals` | `listOrderOpsApprovals` |
| 审批通过 | POST | `/api/order-ops/approvals/{approvalId}/confirm` | `confirmOrderOpsApproval` |
| 审批拒绝 | POST | `/api/order-ops/approvals/{approvalId}/reject` | `rejectOrderOpsApproval` |
| 审计日志 | GET | `/api/order-ops/audit-logs` | `listOrderOpsAuditLogs` |
| 补偿重试 | POST | `/api/order-ops/compensations/{compensationId}/retry` | `retryOrderOpsCompensation` |

## 代码地图

| 位置 | 说明 |
|------|------|
| `backend/src/main/java/com/anjing/controller/OrderOpsController.java` | 运行接口入口，全部使用 `ApiConstants.OrderOps` |
| `backend/src/main/java/com/anjing/service/orderops/OrderOpsAgentService.java` | 计划、执行、审批、幂等、补偿的核心编排 |
| `backend/src/main/java/com/anjing/service/orderops/OrderOpsToolRegistry.java` | 工具注册表和工具风险边界 |
| `backend/src/main/java/com/anjing/service/orderops/OrderOpsAuditService.java` | 审计日志与补偿任务记录 |
| `backend/src/main/java/com/anjing/service/orderops/OrderOpsStore.java` | 内存态订单、审批、幂等和补偿存储 |
| `frontend/src/api/model/orderOpsModel.ts` | 从 OpenAPI operation 类型派生前端模型 |
| `frontend/src/api/order-ops.ts` | 前端运行 API wrapper |
| `frontend/src/views/order-ops/console/index.vue` | 订单运营执行台 |

## 质量门禁

修改后至少运行：

```bash
node scripts/check-service-boundaries.js
node scripts/check-openapi-contract.js
node scripts/check-frontend-openapi-boundaries.js
(cd backend && mvn test)
(cd frontend && pnpm build)
```

如果修改了 Controller、DTO、VO 或 service boundary，需要重新从真实 `/v3/api-docs` 生成前端 OpenAPI 类型，并运行：

```bash
node scripts/generate-openapi-frontend-types.js /path/to/openapi.json --check
node scripts/check-openapi-runtime-contract.js /path/to/openapi.json
```
