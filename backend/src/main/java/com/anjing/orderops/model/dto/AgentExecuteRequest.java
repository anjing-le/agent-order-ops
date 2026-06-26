package com.anjing.orderops.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentExecuteRequest extends AgentPlanRequest {

    @NotBlank
    private String idempotencyKey;

    private Boolean confirmRisk;

    private String simulateFailureStage;
}
