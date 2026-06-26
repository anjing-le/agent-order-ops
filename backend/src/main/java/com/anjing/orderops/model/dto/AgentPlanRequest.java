package com.anjing.orderops.model.dto;

import com.anjing.orderops.enums.OrderActionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AgentPlanRequest {

    @NotBlank
    private String orderNo;

    @NotNull
    private OrderActionType actionType;

    private String newAddress;

    @DecimalMin("0.01")
    private BigDecimal refundAmount;

    @DecimalMin("0.01")
    private BigDecimal couponAmount;

    private String reason;

    private String operatorId;

    private String operatorName;
}
