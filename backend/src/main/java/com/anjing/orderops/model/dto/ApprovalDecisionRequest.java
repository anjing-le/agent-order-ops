package com.anjing.orderops.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalDecisionRequest {

    @NotBlank
    private String operatorId;

    private String operatorName;

    private String comment;
}
