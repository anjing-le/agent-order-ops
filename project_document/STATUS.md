# Status

更新时间：2026-06-26

本文记录 `agent-order-ops` 从 `infra-dev-scaffolding` 重构为订单运营执行 Agent 教学项目后的阶段状态、验证证据和后续方向。脚手架基线快照来自 2026-06-07，保留其契约和门禁资产，业务运行面已替换为 `order-ops` 模块。

## 本项目阶段状态

| 阶段 | 状态 | 证据 |
|------|------|------|
| P0 项目初始化 | Ready | 已按脚手架结构初始化 `agent-order-ops`，统一前端包名、后端 artifact、`spring.application.name`、端口、数据库名、远程仓库和服务边界 |
| P1 后端执行 Agent | Ready | 已落地订单查询、工具注册、计划生成、执行编排、审批确认、审计日志、幂等重放和失败补偿；后端 `mvn test` 已通过 |
| P2 OpenAPI 契约 | Ready | `/v3/api-docs` 已包含 `order-ops` operationId，前端 `schemas.ts` / `operations.ts` 已由真实 OpenAPI 生成并通过契约检查 |
| P3 前端执行台 | Ready | `/order-ops/console` 已作为游客登录默认入口，覆盖订单池、计划、执行、审批、审计和补偿视图；`pnpm build` 和浏览器烟测已通过 |
| P4 教学文档 | Ready | `ORDER_OPS_AGENT_GUIDE.md` 记录教学目标、演示路径、工具边界、API 地图、代码地图和质量门禁 |

## 教学能力覆盖

| 业务模块 | 当前状态 | 教学重点 |
|----------|----------|----------|
| 订单查询 | Ready | Agent 先读取订单、支付、履约、风险标签和时间线，再决定后续动作 |
| 发货拦截 | Ready | 高风险副作用动作进入人工确认边界，审批通过后继续执行 |
| 地址修改 | Ready | 根据履约状态和风险等级选择直改、审批或拒绝 |
| 退款申请 | Ready | 校验支付状态、金额和售后边界，演示审批流与审计 |
| 优惠补偿 | Ready | 低风险补偿可直接执行，复用 idempotency key 可观察幂等重放 |
| 审批确认 | Ready | 待审批单支持通过和拒绝，并记录操作者、原因和结果 |
| 执行审计 | Ready | 计划、工具调用、审批、失败、补偿重试都进入审计日志 |

## 当前证据链

本次重构已实际运行过以下关键命令：

```bash
node scripts/check-service-boundaries.js
node scripts/check-backend-context-contract.js
node scripts/check-async-context-contract.js
node scripts/generate-service-boundaries-backend.js --check
node scripts/generate-service-boundaries-frontend.js --check
node scripts/check-openapi-runtime-contract.js /tmp/agent-order-ops-openapi-18090.json
node scripts/generate-openapi-frontend-types.js /tmp/agent-order-ops-openapi-18090.json --check
node scripts/check-openapi-contract.js
node scripts/check-frontend-openapi-boundaries.js
(cd backend && mvn test)
(cd backend && mvn -q -Dtest=RequestContextTaskDecoratorTest test)
(cd frontend && pnpm build)
```

完整发布前仍建议运行脚手架总门禁：

```bash
./scripts/quality-gate.sh
./scripts/check-template.sh
./scripts/check-contracts.sh
./scripts/smoke-copy.sh
(cd backend && mvn -q -DskipTests package)
(cd frontend && pnpm build)
(cd frontend && pnpm -s clean:dev)
```

## 脚手架继承证据

本项目保留 `infra-dev-scaffolding` 的核心工程阶段和检查词典，以便 `check-template.sh` 继续守住复制资产：

| 原阶段 | 当前处理 |
|--------|----------|
| S0 构建与入口收口 | 保留前后端启动、构建和质量门禁，入口从 `/dashboard/console` 调整到 `/order-ops/console` |
| S1 工程母版收口 | 保留 `COPY_GUIDE.md`、`PROJECT_CONSTRAINTS.md`、`NEW_MODULE_GUIDE.md`、`UI_DESIGN_GUIDE.md`、`DEMO_EVIDENCE.md` 和 CI 模板 |
| S2 AI 协作资产收口 | 保留 Cursor Rules / Prompts、`SCAFFOLD_ADOPTION_PROMPT.md` 和新增模块契约 |
| S3 后续项目复用验证 | 保留 `infra-skill-hub` 复用经验作为脚手架继承背景，本项目不继续沉淀 Skill Hub 领域能力 |

关键继承约束：

- `SCAFFOLD_ADOPTION_PROMPT.md` 仍是让 Codex / Cursor 接手本工程的入口。
- `contracts/service-boundaries.json` 仍是服务边界事实来源，`check-service-boundaries.js` 继续校验后端常量、前端路径、Controller 和 OpenAPI 标记一致。
- `/v3/api-docs` 仍是真实运行接口契约来源。
- 分页字段继续使用 `records/current/size/total`。
- 前端删除请求继续使用 `request.del`。
- 新页面和 API 层继续遵守 OpenAPI 生成物导入边界。

## 下一步

1. 补充 `docs/evidence/YYYY-MM-DD/` 下的正式截图、后端 probe 输出和执行台演示录屏。
2. 将当前内存态 `OrderOpsStore` 替换为数据库表时，保持 DTO / VO、OpenAPI operationId 和前端 API model 不变。
3. 接入真实外部履约、支付或优惠券服务前，先把工具 adapter 做成可替换接口，并继续保留本地轻启动样例。
