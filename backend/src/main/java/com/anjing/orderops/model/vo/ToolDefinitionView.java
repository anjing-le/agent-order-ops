package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinitionView {

    private String toolName;

    private String title;

    private OrderActionType actionType;

    private RiskLevel riskLevel;

    private String description;

    private String requiresApprovalHint;

    private String idempotencyHint;
}
