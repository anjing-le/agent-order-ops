package com.anjing.orderops.service;

import com.anjing.model.errorcode.OrderOpsErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.orderops.enums.ApprovalStatus;
import com.anjing.orderops.enums.AuditEventType;
import com.anjing.orderops.enums.CompensationStatus;
import com.anjing.orderops.enums.ExecutionStatus;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.enums.PaymentStatus;
import com.anjing.orderops.enums.RiskLevel;
import com.anjing.orderops.enums.ShipmentStatus;
import com.anjing.orderops.enums.ToolCallStatus;
import com.anjing.orderops.model.domain.ApprovalTicket;
import com.anjing.orderops.model.domain.IdempotencyRecord;
import com.anjing.orderops.model.domain.OrderRecord;
import com.anjing.orderops.model.domain.ToolStepDefinition;
import com.anjing.orderops.model.dto.AgentExecuteRequest;
import com.anjing.orderops.model.dto.AgentPlanRequest;
import com.anjing.orderops.model.dto.ApprovalDecisionRequest;
import com.anjing.orderops.model.vo.AgentPlanView;
import com.anjing.orderops.model.vo.ApprovalTicketView;
import com.anjing.orderops.model.vo.AuditLogView;
import com.anjing.orderops.model.vo.CompensationTaskView;
import com.anjing.orderops.model.vo.ExecutionResultView;
import com.anjing.orderops.model.vo.OrderTimelineItem;
import com.anjing.orderops.model.vo.OrderView;
import com.anjing.orderops.model.vo.ToolCallView;
import com.anjing.orderops.model.vo.ToolDefinitionView;
import com.anjing.util.DateUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderOpsAgentService {

    private static final BigDecimal REFUND_APPROVAL_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal COUPON_APPROVAL_THRESHOLD = new BigDecimal("50.00");

    private final OrderOpsStore store;
    private final OrderOpsToolRegistry toolRegistry;
    private final OrderOpsAuditService auditService;

    public List<OrderView> listOrders(String keyword, OrderStatus orderStatus) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return store.listOrders().stream()
                .filter(order -> orderStatus == null || order.getOrderStatus() == orderStatus)
                .filter(order -> normalizedKeyword == null
                        || order.getOrderNo().contains(normalizedKeyword)
                        || order.getCustomerName().contains(normalizedKeyword)
                        || order.getPhoneMasked().contains(normalizedKeyword))
                .map(this::toOrderView)
                .toList();
    }

    public OrderView getOrder(String orderNo) {
        OrderRecord order = getOrderRecord(orderNo);
        auditService.record(AuditEventType.ORDER_QUERIED, orderNo, OrderActionType.QUERY_ORDER,
                "ops-agent-demo", "查询订单上下文", "读取订单详情用于运营判断");
        return toOrderView(order);
    }

    public List<ToolDefinitionView> listTools() {
        return toolRegistry.listTools();
    }

    public AgentPlanView plan(AgentPlanRequest request) {
        OrderRecord order = getOrderRecord(request.getOrderNo());
        validateActionPayload(order, request);
        List<ToolStepDefinition> steps = toolRegistry.planSteps(request, order);
        RiskLevel riskLevel = assessRisk(order, request);
        boolean requiresApproval = requiresApproval(order, request);
        AgentPlanView plan = AgentPlanView.builder()
                .planId(store.nextPlanId())
                .orderNo(order.getOrderNo())
                .actionType(request.getActionType())
                .riskLevel(riskLevel)
                .requiresApproval(requiresApproval)
                .approvalReason(approvalReason(order, request, requiresApproval))
                .summary(planSummary(order, request, riskLevel, requiresApproval))
                .toolCalls(toolRegistry.toPendingCalls(steps))
                .humanBoundary(humanBoundary(requiresApproval))
                .compensationPlan(toolRegistry.compensationAction(request.getActionType()))
                .build();
        auditService.record(AuditEventType.PLAN_CREATED, order.getOrderNo(), request.getActionType(),
                operatorId(request), "生成执行计划", plan.getSummary());
        return plan;
    }

    public synchronized ExecutionResultView execute(AgentExecuteRequest request) {
        String idempotencyKey = request.getIdempotencyKey().trim();
        String fingerprint = fingerprint(request);
        Optional<IdempotencyRecord> existingRecord = store.findIdempotencyRecord(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.getFingerprint().equals(fingerprint)) {
                throw new BizException(OrderOpsErrorCode.IDEMPOTENCY_CONFLICT);
            }
            ExecutionResultView replayResult = copyResult(record.getResult(), true);
            auditService.record(AuditEventType.IDEMPOTENCY_REPLAYED, request.getOrderNo(), request.getActionType(),
                    operatorId(request), "命中幂等回放", "idempotencyKey=" + idempotencyKey);
            return replayResult;
        }

        AgentPlanView plan = plan(request);
        ExecutionResultView result = plan.isRequiresApproval()
                ? createApproval(request, plan, idempotencyKey)
                : performExecution(request, plan, store.nextExecutionId(), null);
        store.saveIdempotencyRecord(IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .fingerprint(fingerprint)
                .result(result)
                .createdAt(DateUtils.nowIso())
                .updatedAt(DateUtils.nowIso())
                .build());
        return result;
    }

    public List<ApprovalTicketView> listApprovals(ApprovalStatus status) {
        return store.listApprovals(status).stream()
                .map(this::toApprovalView)
                .toList();
    }

    public synchronized ApprovalTicketView confirmApproval(String approvalId, ApprovalDecisionRequest request) {
        ApprovalTicket ticket = getApprovalTicket(approvalId);
        if (ticket.getStatus() != ApprovalStatus.PENDING) {
            throw new BizException(OrderOpsErrorCode.APPROVAL_ALREADY_HANDLED);
        }
        ticket.setStatus(ApprovalStatus.APPROVED);
        ticket.setDecidedBy(operatorLabel(request.getOperatorId(), request.getOperatorName()));
        ticket.setDecidedAt(DateUtils.nowIso());
        ticket.setDecisionComment(request.getComment());
        auditService.record(AuditEventType.APPROVAL_CONFIRMED, ticket.getOrderNo(), ticket.getActionType(),
                request.getOperatorId(), "审批通过", safeText(request.getComment(), "人工确认允许继续执行"));

        String executionId = ticket.getResult() == null ? store.nextExecutionId() : ticket.getResult().getExecutionId();
        ExecutionResultView result = performExecution(ticket.getOriginalRequest(), ticket.getPlan(),
                executionId, ticket.getApprovalId());
        ticket.setResult(result);
        store.saveApproval(ticket);
        updateIdempotencyResult(ticket.getIdempotencyKey(), result);
        return toApprovalView(ticket);
    }

    public synchronized ApprovalTicketView rejectApproval(String approvalId, ApprovalDecisionRequest request) {
        ApprovalTicket ticket = getApprovalTicket(approvalId);
        if (ticket.getStatus() != ApprovalStatus.PENDING) {
            throw new BizException(OrderOpsErrorCode.APPROVAL_ALREADY_HANDLED);
        }
        ticket.setStatus(ApprovalStatus.REJECTED);
        ticket.setDecidedBy(operatorLabel(request.getOperatorId(), request.getOperatorName()));
        ticket.setDecidedAt(DateUtils.nowIso());
        ticket.setDecisionComment(request.getComment());
        ExecutionResultView result = copyResult(ticket.getResult(), false);
        result.setStatus(ExecutionStatus.FAILED);
        result.setSummary("审批拒绝，Agent 未执行任何有副作用的工具调用");
        ticket.setResult(result);
        store.saveApproval(ticket);
        updateIdempotencyResult(ticket.getIdempotencyKey(), result);
        auditService.record(AuditEventType.APPROVAL_REJECTED, ticket.getOrderNo(), ticket.getActionType(),
                request.getOperatorId(), "审批拒绝", safeText(request.getComment(), "人工拒绝执行"));
        return toApprovalView(ticket);
    }

    public List<AuditLogView> listAuditLogs(String orderNo, int limit) {
        return auditService.list(orderNo, limit);
    }

    public synchronized CompensationTaskView retryCompensation(String compensationId, ApprovalDecisionRequest request) {
        CompensationTaskView compensation = store.findCompensation(compensationId)
                .orElseThrow(() -> new BizException(OrderOpsErrorCode.COMPENSATION_NOT_FOUND));
        if (compensation.getStatus() == CompensationStatus.SUCCEEDED) {
            throw new BizException(OrderOpsErrorCode.COMPENSATION_NOT_RETRYABLE);
        }
        compensation.setRetryCount(compensation.getRetryCount() + 1);
        compensation.setStatus(CompensationStatus.SUCCEEDED);
        compensation.setLastError(null);
        store.saveCompensation(compensation);
        auditService.record(AuditEventType.COMPENSATION_RETRIED, compensation.getOrderNo(),
                compensation.getActionType(), request.getOperatorId(), "重试补偿任务",
                compensation.getCompensationAction());
        auditService.record(AuditEventType.COMPENSATION_SUCCEEDED, compensation.getOrderNo(),
                compensation.getActionType(), request.getOperatorId(), "补偿任务完成",
                "compensationId=" + compensationId);
        return compensation;
    }

    private ExecutionResultView createApproval(AgentExecuteRequest request, AgentPlanView plan,
            String idempotencyKey) {
        String approvalId = store.nextApprovalId();
        ExecutionResultView waitingResult = ExecutionResultView.builder()
                .executionId(store.nextExecutionId())
                .idempotencyKey(idempotencyKey)
                .orderNo(request.getOrderNo())
                .actionType(request.getActionType())
                .status(ExecutionStatus.WAITING_APPROVAL)
                .approvalId(approvalId)
                .summary("命中人工确认边界，已创建审批单 " + approvalId)
                .replay(false)
                .toolCalls(copyToolCalls(plan.getToolCalls()))
                .build();
        ApprovalTicket ticket = ApprovalTicket.builder()
                .approvalId(approvalId)
                .idempotencyKey(idempotencyKey)
                .orderNo(request.getOrderNo())
                .actionType(request.getActionType())
                .status(ApprovalStatus.PENDING)
                .riskLevel(plan.getRiskLevel())
                .reason(plan.getApprovalReason())
                .requestedBy(operatorLabel(operatorId(request), request.getOperatorName()))
                .requestedAt(DateUtils.nowIso())
                .plan(plan)
                .originalRequest(copyExecuteRequest(request))
                .result(waitingResult)
                .build();
        store.saveApproval(ticket);
        auditService.record(AuditEventType.APPROVAL_CREATED, request.getOrderNo(), request.getActionType(),
                operatorId(request), "创建审批单", plan.getApprovalReason());
        return waitingResult;
    }

    private ExecutionResultView performExecution(AgentExecuteRequest request, AgentPlanView plan,
            String executionId, String approvalId) {
        OrderRecord order = getOrderRecord(request.getOrderNo());
        List<ToolStepDefinition> steps = toolRegistry.planSteps(request, order);
        List<ToolCallView> calls = new ArrayList<>();

        for (int index = 0; index < steps.size(); index++) {
            ToolStepDefinition step = steps.get(index);
            ToolCallView call = ToolCallView.builder()
                    .toolName(step.getToolName())
                    .stepName(step.getStepName())
                    .status(ToolCallStatus.RUNNING)
                    .inputSummary(step.getInputSummary())
                    .startedAt(DateUtils.nowIso())
                    .build();
            calls.add(call);
            auditService.record(AuditEventType.TOOL_CALL_STARTED, order.getOrderNo(), request.getActionType(),
                    operatorId(request), "开始调用工具", step.getToolName());

            if (shouldSimulateFailure(request, step, index + 1)) {
                return failExecution(request, executionId, approvalId, calls, call, step,
                        "模拟失败：" + step.getToolName());
            }

            try {
                String output = toolRegistry.executeStep(step, request, order);
                call.setStatus(ToolCallStatus.SUCCESS);
                call.setOutputSummary(output);
                call.setFinishedAt(DateUtils.nowIso());
                auditService.record(AuditEventType.TOOL_CALL_SUCCEEDED, order.getOrderNo(), request.getActionType(),
                        operatorId(request), "工具调用成功", step.getToolName() + " -> " + output);
            } catch (RuntimeException ex) {
                return failExecution(request, executionId, approvalId, calls, call, step,
                        safeText(ex.getMessage(), "工具调用异常"));
            }
        }

        ExecutionResultView result = ExecutionResultView.builder()
                .executionId(executionId)
                .idempotencyKey(request.getIdempotencyKey())
                .orderNo(request.getOrderNo())
                .actionType(request.getActionType())
                .status(ExecutionStatus.SUCCESS)
                .approvalId(approvalId)
                .summary("执行成功：" + plan.getSummary())
                .replay(false)
                .toolCalls(calls)
                .build();
        auditService.record(AuditEventType.EXECUTION_SUCCEEDED, request.getOrderNo(), request.getActionType(),
                operatorId(request), "执行成功", result.getSummary());
        return result;
    }

    private ExecutionResultView failExecution(AgentExecuteRequest request, String executionId, String approvalId,
            List<ToolCallView> calls, ToolCallView failedCall, ToolStepDefinition step, String errorMessage) {
        failedCall.setStatus(ToolCallStatus.FAILED);
        failedCall.setOutputSummary(errorMessage);
        failedCall.setFinishedAt(DateUtils.nowIso());
        CompensationTaskView compensation = createCompensationIfNeeded(request, step, errorMessage);
        auditService.record(AuditEventType.TOOL_CALL_FAILED, request.getOrderNo(), request.getActionType(),
                operatorId(request), "工具调用失败", step.getToolName() + " -> " + errorMessage);
        auditService.record(AuditEventType.EXECUTION_FAILED, request.getOrderNo(), request.getActionType(),
                operatorId(request), "执行失败", errorMessage);
        return ExecutionResultView.builder()
                .executionId(executionId)
                .idempotencyKey(request.getIdempotencyKey())
                .orderNo(request.getOrderNo())
                .actionType(request.getActionType())
                .status(ExecutionStatus.FAILED)
                .approvalId(approvalId)
                .summary("执行失败，已生成可跟踪的补偿任务")
                .replay(false)
                .toolCalls(calls)
                .compensationTask(compensation)
                .build();
    }

    private CompensationTaskView createCompensationIfNeeded(AgentExecuteRequest request,
            ToolStepDefinition step, String errorMessage) {
        if (request.getActionType() == OrderActionType.QUERY_ORDER
                || !toolRegistry.isMutationTool(step.getToolName())
                        && !"write_operation_audit".equals(step.getToolName())) {
            return null;
        }
        CompensationTaskView compensation = CompensationTaskView.builder()
                .compensationId(store.nextCompensationId())
                .orderNo(request.getOrderNo())
                .actionType(request.getActionType())
                .status(CompensationStatus.PENDING)
                .failedStep(step.getToolName())
                .compensationAction(toolRegistry.compensationAction(request.getActionType()))
                .retryCount(0)
                .lastError(errorMessage)
                .createdAt(DateUtils.nowIso())
                .updatedAt(DateUtils.nowIso())
                .build();
        store.saveCompensation(compensation);
        auditService.record(AuditEventType.COMPENSATION_CREATED, request.getOrderNo(), request.getActionType(),
                operatorId(request), "创建补偿任务", compensation.getCompensationAction());
        return compensation;
    }

    private OrderRecord getOrderRecord(String orderNo) {
        return store.findOrder(orderNo)
                .orElseThrow(() -> new BizException(OrderOpsErrorCode.ORDER_NOT_FOUND));
    }

    private ApprovalTicket getApprovalTicket(String approvalId) {
        return store.findApproval(approvalId)
                .orElseThrow(() -> new BizException(OrderOpsErrorCode.APPROVAL_NOT_FOUND));
    }

    private void validateActionPayload(OrderRecord order, AgentPlanRequest request) {
        switch (request.getActionType()) {
            case QUERY_ORDER -> {
                return;
            }
            case INTERCEPT_SHIPMENT -> {
                if (!canIntercept(order)) {
                    throw new BizException(OrderOpsErrorCode.ACTION_NOT_ALLOWED);
                }
            }
            case CHANGE_ADDRESS -> {
                if (!StringUtils.hasText(request.getNewAddress())) {
                    throw new BizException(OrderOpsErrorCode.INVALID_ACTION_PAYLOAD);
                }
                if (!canChangeAddress(order)) {
                    throw new BizException(OrderOpsErrorCode.ACTION_NOT_ALLOWED);
                }
            }
            case APPLY_REFUND -> {
                if (request.getRefundAmount() == null
                        || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException(OrderOpsErrorCode.INVALID_ACTION_PAYLOAD);
                }
                if (!canRefund(order) || request.getRefundAmount().compareTo(order.getRefundableAmount()) > 0) {
                    throw new BizException(OrderOpsErrorCode.ACTION_NOT_ALLOWED);
                }
            }
            case ISSUE_COUPON -> {
                if (request.getCouponAmount() == null
                        || request.getCouponAmount().compareTo(BigDecimal.ZERO) <= 0
                        || !StringUtils.hasText(request.getReason())) {
                    throw new BizException(OrderOpsErrorCode.INVALID_ACTION_PAYLOAD);
                }
            }
            default -> throw new BizException(OrderOpsErrorCode.ACTION_NOT_ALLOWED);
        }
    }

    private RiskLevel assessRisk(OrderRecord order, AgentPlanRequest request) {
        return switch (request.getActionType()) {
            case QUERY_ORDER -> RiskLevel.LOW;
            case ISSUE_COUPON -> request.getCouponAmount().compareTo(COUPON_APPROVAL_THRESHOLD) > 0
                    ? RiskLevel.MEDIUM : RiskLevel.LOW;
            case APPLY_REFUND -> requiresApproval(order, request) ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            case INTERCEPT_SHIPMENT -> order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT
                    ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            case CHANGE_ADDRESS -> order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT
                    ? RiskLevel.HIGH
                    : order.getShipmentStatus() == ShipmentStatus.ALLOCATED ? RiskLevel.MEDIUM : RiskLevel.LOW;
        };
    }

    private boolean requiresApproval(OrderRecord order, AgentPlanRequest request) {
        return switch (request.getActionType()) {
            case QUERY_ORDER -> false;
            case INTERCEPT_SHIPMENT -> order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT;
            case CHANGE_ADDRESS -> order.getShipmentStatus() == ShipmentStatus.ALLOCATED
                    || order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT;
            case APPLY_REFUND -> request.getRefundAmount().compareTo(REFUND_APPROVAL_THRESHOLD) > 0
                    || order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT
                    || order.getShipmentStatus() == ShipmentStatus.DELIVERED;
            case ISSUE_COUPON -> request.getCouponAmount().compareTo(COUPON_APPROVAL_THRESHOLD) > 0;
        };
    }

    private String approvalReason(OrderRecord order, AgentPlanRequest request, boolean requiresApproval) {
        if (!requiresApproval) {
            return "未触发人工审批边界";
        }
        return switch (request.getActionType()) {
            case INTERCEPT_SHIPMENT -> "运输中包裹拦截会影响履约承诺，需要人工确认。";
            case CHANGE_ADDRESS -> "订单已进入履约链路，改地址需要人工确认客户意愿和仓配可行性。";
            case APPLY_REFUND -> "退款金额或订单状态触发高风险售后边界，需要人工审批。";
            case ISSUE_COUPON -> "优惠补偿超过自动发放额度，需要人工审批。";
            case QUERY_ORDER -> "只读查询不需要审批。";
        };
    }

    private String planSummary(OrderRecord order, AgentPlanRequest request, RiskLevel riskLevel,
            boolean requiresApproval) {
        return "订单 " + order.getOrderNo() + " 将执行 " + request.getActionType()
                + "，风险等级 " + riskLevel
                + (requiresApproval ? "，需人工审批后继续。" : "，可直接执行。");
    }

    private String humanBoundary(boolean requiresApproval) {
        return requiresApproval
                ? "Agent 只能生成计划和审批单，必须由人工审批确认后才能调用有副作用的业务工具。"
                : "Agent 可在幂等键保护下直接执行，失败时写入补偿任务和审计日志。";
    }

    private boolean shouldSimulateFailure(AgentExecuteRequest request, ToolStepDefinition step, int stepIndex) {
        String marker = request.getSimulateFailureStage();
        return StringUtils.hasText(marker)
                && (marker.equalsIgnoreCase(step.getToolName())
                        || marker.equalsIgnoreCase(step.getStepName())
                        || marker.equals(String.valueOf(stepIndex)));
    }

    private boolean canIntercept(OrderRecord order) {
        return order.getShipmentStatus() == ShipmentStatus.PENDING
                || order.getShipmentStatus() == ShipmentStatus.ALLOCATED
                || order.getShipmentStatus() == ShipmentStatus.IN_TRANSIT;
    }

    private boolean canChangeAddress(OrderRecord order) {
        return order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getShipmentStatus() != ShipmentStatus.DELIVERED
                && order.getShipmentStatus() != ShipmentStatus.INTERCEPTED;
    }

    private boolean canRefund(OrderRecord order) {
        return (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                && order.getRefundableAmount() != null
                && order.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private OrderView toOrderView(OrderRecord order) {
        return OrderView.builder()
                .orderNo(order.getOrderNo())
                .customerName(order.getCustomerName())
                .phoneMasked(order.getPhoneMasked())
                .address(order.getAddress())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .shipmentStatus(order.getShipmentStatus())
                .paidAmount(order.getPaidAmount())
                .refundableAmount(order.getRefundableAmount())
                .refundedAmount(order.getRefundedAmount())
                .couponIssuedAmount(order.getCouponIssuedAmount())
                .riskTags(order.getRiskTags())
                .timeline(order.getTimeline().stream()
                        .sorted(Comparator.comparing(OrderTimelineItem::getTime).reversed())
                        .toList())
                .canIntercept(canIntercept(order))
                .canChangeAddress(canChangeAddress(order))
                .canRefund(canRefund(order))
                .canCompensate(true)
                .build();
    }

    private ApprovalTicketView toApprovalView(ApprovalTicket approval) {
        return ApprovalTicketView.builder()
                .approvalId(approval.getApprovalId())
                .orderNo(approval.getOrderNo())
                .actionType(approval.getActionType())
                .status(approval.getStatus())
                .riskLevel(approval.getRiskLevel())
                .reason(approval.getReason())
                .requestedBy(approval.getRequestedBy())
                .requestedAt(approval.getRequestedAt())
                .decidedBy(approval.getDecidedBy())
                .decidedAt(approval.getDecidedAt())
                .decisionComment(approval.getDecisionComment())
                .plan(approval.getPlan())
                .result(approval.getResult())
                .build();
    }

    private void updateIdempotencyResult(String idempotencyKey, ExecutionResultView result) {
        store.findIdempotencyRecord(idempotencyKey).ifPresent(record -> {
            record.setResult(result);
            record.setUpdatedAt(DateUtils.nowIso());
            store.saveIdempotencyRecord(record);
        });
    }

    private ExecutionResultView copyResult(ExecutionResultView source, boolean replay) {
        if (source == null) {
            return ExecutionResultView.builder().replay(replay).build();
        }
        return ExecutionResultView.builder()
                .executionId(source.getExecutionId())
                .idempotencyKey(source.getIdempotencyKey())
                .orderNo(source.getOrderNo())
                .actionType(source.getActionType())
                .status(source.getStatus())
                .approvalId(source.getApprovalId())
                .summary(source.getSummary())
                .replay(replay)
                .toolCalls(copyToolCalls(source.getToolCalls()))
                .compensationTask(source.getCompensationTask())
                .build();
    }

    private List<ToolCallView> copyToolCalls(List<ToolCallView> toolCalls) {
        if (toolCalls == null) {
            return List.of();
        }
        return toolCalls.stream()
                .map(call -> ToolCallView.builder()
                        .toolName(call.getToolName())
                        .stepName(call.getStepName())
                        .status(call.getStatus())
                        .inputSummary(call.getInputSummary())
                        .outputSummary(call.getOutputSummary())
                        .startedAt(call.getStartedAt())
                        .finishedAt(call.getFinishedAt())
                        .build())
                .toList();
    }

    private AgentExecuteRequest copyExecuteRequest(AgentExecuteRequest request) {
        AgentExecuteRequest copy = new AgentExecuteRequest();
        copy.setOrderNo(request.getOrderNo());
        copy.setActionType(request.getActionType());
        copy.setNewAddress(request.getNewAddress());
        copy.setRefundAmount(request.getRefundAmount());
        copy.setCouponAmount(request.getCouponAmount());
        copy.setReason(request.getReason());
        copy.setOperatorId(request.getOperatorId());
        copy.setOperatorName(request.getOperatorName());
        copy.setIdempotencyKey(request.getIdempotencyKey());
        copy.setConfirmRisk(request.getConfirmRisk());
        copy.setSimulateFailureStage(request.getSimulateFailureStage());
        return copy;
    }

    private String fingerprint(AgentExecuteRequest request) {
        return String.join("|",
                safeText(request.getOrderNo(), ""),
                String.valueOf(request.getActionType()),
                safeText(request.getNewAddress(), ""),
                amountText(request.getRefundAmount()),
                amountText(request.getCouponAmount()),
                safeText(request.getReason(), ""),
                safeText(request.getSimulateFailureStage(), ""));
    }

    private String amountText(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private String operatorId(AgentPlanRequest request) {
        return StringUtils.hasText(request.getOperatorId()) ? request.getOperatorId().trim() : "ops-agent-demo";
    }

    private String operatorLabel(String operatorId, String operatorName) {
        if (StringUtils.hasText(operatorName)) {
            return operatorName.trim() + " (" + operatorId + ")";
        }
        return operatorId;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
