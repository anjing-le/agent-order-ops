package com.anjing.orderops.model.domain;

import com.anjing.orderops.enums.ApprovalStatus;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.RiskLevel;
import com.anjing.orderops.model.dto.AgentExecuteRequest;
import com.anjing.orderops.model.vo.AgentPlanView;
import com.anjing.orderops.model.vo.ExecutionResultView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTicket {

    private String approvalId;

    private String idempotencyKey;

    private String orderNo;

    private OrderActionType actionType;

    private ApprovalStatus status;

    private RiskLevel riskLevel;

    private String reason;

    private String requestedBy;

    private String requestedAt;

    private String decidedBy;

    private String decidedAt;

    private String decisionComment;

    private AgentPlanView plan;

    private AgentExecuteRequest originalRequest;

    private ExecutionResultView result;
}
