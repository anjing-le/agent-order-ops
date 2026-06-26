package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.RiskLevel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPlanView {

    private String planId;

    private String orderNo;

    private OrderActionType actionType;

    private RiskLevel riskLevel;

    private boolean requiresApproval;

    private String approvalReason;

    private String summary;

    private List<ToolCallView> toolCalls;

    private String humanBoundary;

    private String compensationPlan;
}
