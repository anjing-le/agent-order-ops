# Roadmap

## 定位

`agent-order-ops` 是一个执行型 Agent 教学项目，面向订单运营场景，展示 Agent 如何把运营意图拆成可审计、可审批、可幂等、可补偿的业务动作。

项目基于 `infra-dev-scaffolding` 初始化，继续使用 Vue 3.5、TypeScript、Vite 7、Spring Boot 3.4.5、Java 17、OpenAPI、H2 / MySQL-ready 的技术栈，不引入新的应用框架。

## 目标

1. 让学习者看到 Tool Calling 不是“调用函数”这么简单，而是要有工具边界、副作用声明、输入输出和审计。
2. 用订单查询、发货拦截、地址修改、退款申请和优惠补偿覆盖常见运营执行动作。
3. 明确人工确认边界：高风险动作先生成审批单，审批通过后才继续执行。
4. 用 idempotency key 演示重复请求如何重放结果或阻断冲突。
5. 用失败注入和补偿重试演示执行链路的恢复设计。
6. 保留脚手架契约，让后续 Agent 教学项目可以继续复用这套工程习惯。
7. 日志统一输出 requestId、traceId、用户、租户、路径、耗时和错误码，让执行动作可以被追踪和复盘。

## 非目标

- 不做真实电商 OMS / WMS / 支付系统。
- 不接入真实 LLM Provider；本项目重点是可控的执行编排和边界设计。
- 不把认证中心、网关、消息队列、注册中心等平台能力扩张成本项目主线。
- 不为了演示效果绕开 OpenAPI、服务边界、审计、幂等或审批约束。

## 阶段规划

### P0: 脚手架接入

目标：仓库身份、端口、数据库名、服务边界和质量门禁全部切换到 `agent-order-ops`。

验收：
- Git author 使用 `安静 <245548353+anjing-le@users.noreply.github.com>`。
- remote 指向 `git@github.com:anjing-le/agent-order-ops.git`。
- 前端端口为 `13016`，后端端口为 `18090`。
- `contracts/service-boundaries.json` 中存在 `order-ops` runtime boundary。
- service boundary 生成和检查通过。

### P1: 后端执行链路

目标：提供可演示的订单运营 Agent 运行接口。

验收：
- 支持订单列表、订单详情和工具注册表查询。
- 支持生成执行计划和执行 Agent 动作。
- 支持审批查询、审批通过、审批拒绝。
- 支持审计日志查询和失败补偿重试。
- 后端测试覆盖幂等重放、幂等冲突、审批确认和补偿重试。

### P2: OpenAPI 与前端 API

目标：前端不手写运行 payload 类型，统一从真实 OpenAPI 生成类型。

验收：
- `/v3/api-docs` 暴露 `order-ops` operationId。
- `frontend/src/contracts/openapi/schemas.ts` 和 `operations.ts` 已生成。
- `frontend/src/api/model/orderOpsModel.ts` 从 OpenAPI operation 类型派生。
- `frontend/src/api/order-ops.ts` 使用 `openApiRequest(operationId)` 调用后端。
- `scripts/check-async-context-contract.js` 和上下文契约检查继续通过，保证异步执行、远程调用和审计上下文不丢失。

### P3: 执行台

目标：提供可直接教学演示的操作界面。

验收：
- 游客登录默认进入 `/order-ops/console`。
- 页面包含订单池、计划生成、执行结果、工具调用、审批列表、审计日志和补偿任务。
- 支持 idempotency key 手动输入、人工确认开关和失败注入开关。
- 前端构建和浏览器烟测通过。

### P4: 教学资产

目标：让学习者不读完整代码也能理解执行型 Agent 的关键设计。

验收：
- README 有快速启动、业务入口和教学路径。
- `ORDER_OPS_AGENT_GUIDE.md` 覆盖 Tool Calling、动作编排、审批、幂等、审计、补偿和人工确认边界。
- `STATUS.md` 记录当前阶段和验证证据。

## 成功标准

学习者完成一次本地演示后，应该能回答：

1. 一个执行型 Agent 的工具注册表应该表达哪些副作用和风险信息。
2. 为什么退款、地址修改、发货拦截不能全部自动执行。
3. 幂等 key 如何保护“重复点击”和“网络重试”。
4. 审计日志应该记录计划、工具调用、审批和补偿中的哪些信息。
5. 失败补偿和人工确认分别解决什么边界问题。
