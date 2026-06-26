package com.anjing.orderops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anjing.model.errorcode.OrderOpsErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.orderops.enums.ApprovalStatus;
import com.anjing.orderops.enums.AuditEventType;
import com.anjing.orderops.enums.CompensationStatus;
import com.anjing.orderops.enums.ExecutionStatus;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.model.dto.AgentExecuteRequest;
import com.anjing.orderops.model.dto.ApprovalDecisionRequest;
import com.anjing.orderops.model.vo.ApprovalTicketView;
import com.anjing.orderops.model.vo.CompensationTaskView;
import com.anjing.orderops.model.vo.ExecutionResultView;
import com.anjing.orderops.service.OrderOpsAgentService;
import com.anjing.orderops.service.OrderOpsAuditService;
import com.anjing.orderops.service.OrderOpsStore;
import com.anjing.orderops.service.OrderOpsToolRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderOpsAgentServiceTest {

    private OrderOpsAgentService service;

    @BeforeEach
    void setUp() {
        OrderOpsStore store = new OrderOpsStore();
        OrderOpsToolRegistry toolRegistry = new OrderOpsToolRegistry(store);
        OrderOpsAuditService auditService = new OrderOpsAuditService(store);
        service = new OrderOpsAgentService(store, toolRegistry, auditService);
    }

    @Test
    void executeReplaysSameIdempotentCouponRequest() {
        AgentExecuteRequest request = couponRequest("coupon-idem-1", new BigDecimal("20.00"), null);

        ExecutionResultView first = service.execute(request);
        ExecutionResultView replay = service.execute(request);

        assertThat(first.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(replay.isReplay()).isTrue();
        assertThat(replay.getExecutionId()).isEqualTo(first.getExecutionId());
        assertThat(service.listAuditLogs(null, 50))
                .extracting("eventType")
                .contains(AuditEventType.IDEMPOTENCY_REPLAYED);
    }

    @Test
    void highRiskRefundWaitsForApprovalAndRunsAfterConfirm() {
        AgentExecuteRequest request = new AgentExecuteRequest();
        request.setOrderNo("ORD-20260626-1004");
        request.setActionType(OrderActionType.APPLY_REFUND);
        request.setRefundAmount(new BigDecimal("120.00"));
        request.setReason("已签收订单体验补偿退款");
        request.setOperatorId("agent-test");
        request.setIdempotencyKey("refund-approval-1");

        ExecutionResultView waiting = service.execute(request);
        ApprovalTicketView pending = service.listApprovals(ApprovalStatus.PENDING).get(0);

        assertThat(waiting.getStatus()).isEqualTo(ExecutionStatus.WAITING_APPROVAL);
        assertThat(waiting.getApprovalId()).isEqualTo(pending.getApprovalId());

        ApprovalTicketView approved = service.confirmApproval(pending.getApprovalId(), decision());

        assertThat(approved.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approved.getResult().getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(service.getOrder("ORD-20260626-1004").getRefundedAmount())
                .isEqualByComparingTo("120.00");
        assertThat(service.listAuditLogs("ORD-20260626-1004", 50))
                .extracting("eventType")
                .contains(AuditEventType.APPROVAL_CONFIRMED, AuditEventType.EXECUTION_SUCCEEDED);
    }

    @Test
    void simulatedFailureCreatesRetryableCompensation() {
        AgentExecuteRequest request = couponRequest("coupon-failure-1", new BigDecimal("30.00"),
                "write_operation_audit");

        ExecutionResultView failed = service.execute(request);
        CompensationTaskView compensation = failed.getCompensationTask();

        assertThat(failed.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(compensation).isNotNull();
        assertThat(compensation.getStatus()).isEqualTo(CompensationStatus.PENDING);

        CompensationTaskView retried = service.retryCompensation(compensation.getCompensationId(), decision());

        assertThat(retried.getStatus()).isEqualTo(CompensationStatus.SUCCEEDED);
        assertThat(retried.getRetryCount()).isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeyRejectsDifferentPayload() {
        service.execute(couponRequest("coupon-conflict-1", new BigDecimal("20.00"), null));

        AgentExecuteRequest changedPayload = couponRequest("coupon-conflict-1", new BigDecimal("25.00"), null);

        assertThatThrownBy(() -> service.execute(changedPayload))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(OrderOpsErrorCode.IDEMPOTENCY_CONFLICT);
    }

    private AgentExecuteRequest couponRequest(String idempotencyKey, BigDecimal amount,
            String simulateFailureStage) {
        AgentExecuteRequest request = new AgentExecuteRequest();
        request.setOrderNo("ORD-20260626-1001");
        request.setActionType(OrderActionType.ISSUE_COUPON);
        request.setCouponAmount(amount);
        request.setReason("发货前体验安抚");
        request.setOperatorId("agent-test");
        request.setIdempotencyKey(idempotencyKey);
        request.setSimulateFailureStage(simulateFailureStage);
        return request;
    }

    private ApprovalDecisionRequest decision() {
        ApprovalDecisionRequest request = new ApprovalDecisionRequest();
        request.setOperatorId("supervisor-test");
        request.setOperatorName("测试主管");
        request.setComment("测试确认");
        return request;
    }
}
