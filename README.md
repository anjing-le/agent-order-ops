# agent-order-ops

订单运营执行 Agent 教学项目：用一个真实可运行的后台，展示 Tool Calling、业务动作编排、审批流、幂等、审计日志、失败补偿和人工确认边界。

`Vue 3.5` · `TypeScript` · `Vite 7` · `Spring Boot 3.4.5` · `Java 17` · `OpenAPI` · `H2 / MySQL`

## 教学重点

| 场景 | 展示能力 |
| --- | --- |
| 订单查询 | Agent 先读订单、履约、支付和客户上下文 |
| 发货拦截 | 调用履约工具，判断是否需要人工确认 |
| 地址修改 | 对未发货订单直改，对高风险订单进入审批 |
| 退款申请 | 校验金额、支付状态和售后边界 |
| 优惠补偿 | 以低风险补偿演示直接执行和幂等重放 |
| 审批确认 | 明确哪些动作必须人审后才能落地 |
| 执行审计 | 记录计划、工具调用、执行结果、失败补偿 |

## 快速开始

前端：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 `http://localhost:13016`。

后端：

```bash
cd backend
mvn spring-boot:run
```

后端端口是 `18090`。默认 dev profile 使用 H2，本地不需要 MySQL、Redis 或其他中间件。

## 技术栈

| 层 | 技术 |
| --- | --- |
| Frontend | Vue 3.5, TypeScript, Vite 7, Pinia, Vue Router |
| UI | Element Plus, SCSS, Tailwind CSS 4 |
| Backend | Spring Boot 3.4.5, Java 17, Maven |
| Data | H2 for dev/test, MySQL-ready |
| Contract | OpenAPI, generated types, API path checks |
| Governance | Shell/Node scripts, scaffold constraints, CI template |

## 业务入口

| 模块 | 说明 |
| --- | --- |
| 订单查询 | 查看订单状态、支付状态、履约状态和风险标签 |
| Agent 计划 | 根据运营意图生成动作计划与工具调用序列 |
| Agent 执行 | 执行低风险动作，或生成待审批单 |
| 审批确认 | 人工确认高风险退款、地址修改和拦截动作 |
| 审计日志 | 查看每次计划、执行、失败和补偿记录 |

## 工程文档

| 想做什么 | 看这里 |
| --- | --- |
| 复制成新项目 | [COPY_GUIDE.md](./project_document/COPY_GUIDE.md) |
| 让 Codex 接手 | [SCAFFOLD_ADOPTION_PROMPT.md](./project_document/SCAFFOLD_ADOPTION_PROMPT.md) |
| 理解项目约束 | [PROJECT_CONSTRAINTS.md](./project_document/PROJECT_CONSTRAINTS.md) |
| 新增业务模块 | [NEW_MODULE_GUIDE.md](./project_document/NEW_MODULE_GUIDE.md) |
| 调整界面风格 | [UI_DESIGN_GUIDE.md](./project_document/UI_DESIGN_GUIDE.md) |
| 发布前检查 | [DEMO_EVIDENCE.md](./project_document/DEMO_EVIDENCE.md) |

## 质量门禁

重要提交前跑完整门禁：

```bash
./scripts/quality-gate.sh
```

文档或轻量模板调整：

```bash
./scripts/check-template.sh
./scripts/check-contracts.sh
```

GitHub Actions 模板在 [project_document/ci/quality-gate.yml](./project_document/ci/quality-gate.yml)。

## 目录

```text
frontend/           Vue 前端工程
backend/            Spring Boot 后端工程
contracts/          平台契约和服务边界 manifest
project_document/   路线图、约束、指南和状态记录
scripts/            自检、生成、复制和质量门禁脚本
```

## License

MIT. 前端工程基于 Art Design Pro 定制，保留 [frontend/LICENSE](./frontend/LICENSE) 中的上游许可说明。
