# agent-order-ops 功能与架构盘点

本文记录 `agent-order-ops` 当前工程快照，方便后续继续扩展课程、README、演示脚本和视频讲解。当前项目定位是订单运营执行 Agent 教学项目，重点展示从运营意图到 Tool Calling、审批、幂等、审计和失败补偿的完整闭环。

## 一、项目定位

`agent-order-ops` 不是一个只包装聊天入口的 Agent Demo，而是一个可运行、可提交、可演示的执行型 Agent 工程。

它基于统一脚手架 `infra-dev-scaffolding` 生长出来，复用同一套前后端技术栈、接口契约、质量脚本和工程习惯：

| 层级 | 当前选型 |
| --- | --- |
| Frontend | Vue 3.5, TypeScript, Vite 7, Pinia, Vue Router |
| UI | Element Plus, SCSS, Tailwind CSS 4 |
| Backend | Spring Boot 3.4.5, Java 17, Maven |
| Data | dev/test 使用 H2，保持 MySQL-ready |
| Contract | OpenAPI operationId, generated frontend types, path checks |
| Governance | scripts 自检、服务边界、平台契约、质量门禁 |

教学重点是让学员理解：同一个工程底座如何承载不同业务类型的 Agent，而执行型 Agent 的核心难点不在“会不会调用模型”，而在“能不能安全地落业务动作”。

## 二、当前功能 List

### 1. 订单查询

- 支持订单池列表。
- 支持按订单号、客户名、手机号关键字搜索。
- 支持按订单状态过滤。
- 支持查看订单详情、支付状态、履约状态、风险标签和订单时间线。
- 查询订单详情时写入审计日志，强调执行前先读取事实上下文。

### 2. Agent 动作计划

- 支持生成业务动作计划。
- 返回计划摘要、风险等级、是否需要审批、审批原因、人工确认边界、失败补偿方案。
- 返回 Tool Calling 调用序列，前端展示每一步工具名、输入摘要和执行状态。
- 计划阶段不会产生真实业务副作用。

### 3. 发货拦截

- 根据履约状态判断是否可拦截。
- 可拦截状态包括待履约、仓内分配、运输中。
- 运输中包裹触发更高风险等级和人工审批边界。
- 执行成功后更新物流状态，并追加订单时间线。

### 4. 地址修改

- 支持输入新收货地址。
- 已取消、已签收、已拦截等状态会限制修改。
- 仓内拣货或运输中修改地址会触发审批边界。
- 执行成功后更新订单地址，并记录原地址到新地址的变更轨迹。

### 5. 退款申请

- 校验退款金额必须大于 0。
- 校验订单支付状态和可退金额。
- 超过 100 元、运输中、已签收等场景触发高风险审批。
- 执行成功后更新已退金额、可退金额和支付状态。

### 6. 优惠补偿

- 支持发放优惠补偿券。
- 低金额补偿可直接执行，用来演示低风险直执。
- 超过 50 元可触发审批边界。
- 执行成功后累计订单补偿金额，并写入时间线。

### 7. 审批确认

- 支持查询审批单。
- 支持审批通过后继续执行原始业务动作。
- 支持审批拒绝后终止执行，并明确“不调用有副作用工具”。
- 审批结果会回写到对应幂等记录，后续同 key 请求可以重放最终结果。

### 8. 幂等重放

- 执行动作必须携带 `idempotencyKey`。
- 同一个幂等键加相同 payload 会重放第一次结果。
- 同一个幂等键加不同 payload 会抛出幂等冲突。
- 幂等指纹包含订单号、动作类型、地址、退款金额、补偿金额、原因和失败注入点。

### 9. 执行审计

- 记录订单查询、计划创建、审批创建、审批通过、审批拒绝。
- 记录工具调用开始、成功、失败。
- 记录执行成功、执行失败。
- 记录幂等重放、补偿创建、补偿重试、补偿成功。
- 前端提供审计时间线，用于课程复盘和演示。

### 10. 失败补偿

- 前端支持失败注入，用于模拟工具调用失败。
- 对有副作用动作的失败会生成补偿任务。
- 补偿任务记录失败步骤、补偿动作、错误信息和重试次数。
- 支持手动重试补偿，并记录补偿审计。

### 11. 前端执行台

- `/order-ops/console` 是核心演示入口。
- 左侧是订单池和订单详情。
- 中间是动作编排、幂等键、人工确认、失败注入、计划和执行按钮。
- 右侧是审批、审计、补偿三个页签。
- 顶部指标展示样例订单、待审批、补偿任务、审计日志数量。

### 12. 教学与课程物料

- `README.md` 说明项目定位、快速开始、教学路径和质量门禁。
- `project_document/ORDER_OPS_AGENT_GUIDE.md` 说明执行型 Agent 教学主线。
- `docs/course-brand-plan.md` 说明“七大 Agent 实战课”的课程品牌与物料规划。

## 三、代码架构设计

### 1. 总体调用链

```text
Vue 订单运营执行台
  -> frontend/src/api/order-ops.ts
  -> OpenAPI generated model types
  -> backend OrderOpsController
  -> OrderOpsAgentService
  -> OrderOpsToolRegistry / OrderOpsAuditService / OrderOpsStore
```

这条链路刻意保留完整工程分层，让课程能讲清楚“前端交互、接口契约、业务编排、工具副作用、审计状态”之间的关系。

### 2. 后端模块结构

| 路径 | 职责 |
| --- | --- |
| `backend/src/main/java/com/anjing/controller/OrderOpsController.java` | REST API 入口，暴露订单、工具、计划、执行、审批、审计、补偿接口 |
| `backend/src/main/java/com/anjing/orderops/enums/` | 订单、履约、支付、审批、风险、执行、工具状态枚举 |
| `backend/src/main/java/com/anjing/orderops/model/domain/` | 内部领域对象，如订单、审批单、幂等记录、工具步骤 |
| `backend/src/main/java/com/anjing/orderops/model/dto/` | 前端请求入参，如计划请求、执行请求、审批决策请求 |
| `backend/src/main/java/com/anjing/orderops/model/vo/` | 前端展示对象，如订单、计划、执行结果、工具调用、审计日志 |
| `backend/src/main/java/com/anjing/orderops/service/OrderOpsAgentService.java` | 核心编排服务，负责计划、执行、审批、幂等、补偿 |
| `backend/src/main/java/com/anjing/orderops/service/OrderOpsToolRegistry.java` | 工具注册表，定义工具元数据、计划步骤和实际副作用 |
| `backend/src/main/java/com/anjing/orderops/service/OrderOpsAuditService.java` | 统一审计日志写入和查询 |
| `backend/src/main/java/com/anjing/orderops/service/OrderOpsStore.java` | 教学沙箱内存存储，管理订单、审批、幂等、审计、补偿 |

### 3. API 设计

| 能力 | Method | Path | operationId |
| --- | --- | --- | --- |
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

所有接口继续走脚手架统一响应结构 `APIResponse`，接口路径由 `ApiConstants.OrderOps` 管理，便于 OpenAPI 契约检查。

### 4. 前端模块结构

| 路径 | 职责 |
| --- | --- |
| `frontend/src/router/modules/order-ops.ts` | 注册 `/order-ops/console` 路由和菜单元信息 |
| `frontend/src/api/order-ops.ts` | 订单运营 Agent 的 API wrapper |
| `frontend/src/api/model/orderOpsModel.ts` | 基于 OpenAPI operationId 派生前端类型 |
| `frontend/src/views/order-ops/console/index.vue` | 订单运营执行台页面 |

前端页面没有绕过 API 直接写死主要业务结果，而是通过 API wrapper 连接后端状态，适合做端到端演示。

### 5. Tool Calling 设计

当前工具注册表包含：

| 工具 | 动作 | 风险 | 副作用 |
| --- | --- | --- | --- |
| `query_order_context` | 订单查询 | LOW | 无 |
| `intercept_shipment` | 发货拦截 | MEDIUM | 更新物流状态 |
| `change_address` | 地址修改 | MEDIUM | 更新收货地址 |
| `apply_refund` | 退款申请 | HIGH | 更新退款金额和支付状态 |
| `issue_coupon` | 优惠补偿 | LOW | 累计补偿券金额 |
| `write_operation_audit` | 执行审计 | LOW | 写入审计轨迹 |

计划生成时，除只读查询外，通常会形成：

```text
读取订单上下文 -> 调用具体业务工具 -> 写入执行审计
```

这个设计适合教学，因为学员能清楚看到：Agent 不是“直接改订单”，而是先拆计划，再按工具边界执行。

### 6. 状态与数据设计

当前项目使用 `OrderOpsStore` 作为教学沙箱内存存储，管理：

- 样例订单。
- 审批单。
- 幂等记录。
- 审计日志。
- 补偿任务。

内存存储适合教学演示，降低本地启动成本。后续如果要产品化，可以替换为数据库表，并保留上层服务编排接口。

### 7. 测试覆盖

后端已有 `OrderOpsAgentServiceTest`，覆盖核心执行型 Agent 风险点：

- 同 key 同 payload 幂等重放。
- 高风险退款进入审批，审批通过后继续执行。
- 模拟失败后生成可重试补偿任务。
- 同 key 不同 payload 触发幂等冲突。

## 四、当前边界

当前工程是执行型 Agent 教学项目，已经具备完整可演示闭环，但仍是教学沙箱：

- 未接真实 OMS、WMS、支付、物流和优惠券系统。
- 未接真实 LLM 推理服务，当前重点是 Agent 执行链路和工具边界。
- 数据使用内存态，服务重启后演示状态会重置。
- 补偿重试当前是教学级模拟，产品化时需要接真实补偿事务和状态机。

这几个边界是刻意保留的：课程先讲清楚“执行型 Agent 应该如何设计安全业务动作”，再逐步替换真实外部系统。

## 五、后续建议

1. 修正 `project_document/ORDER_OPS_AGENT_GUIDE.md` 中旧的 service 路径描述，让代码地图与当前目录完全一致。
2. 增加一份 `project_document/DEMO_SCRIPT.md`，把优惠补偿、审批退款、失败补偿三条演示路径写成讲课脚本。
3. 给前端执行台补一段课程录屏专用 seed 流程，保证每次演示有稳定的订单、审批和审计初始状态。
4. 后续如要接真实大模型，可优先在 `OrderOpsAgentService.plan` 前增加意图解析层，保持执行层和工具层不被模型输出直接污染。
