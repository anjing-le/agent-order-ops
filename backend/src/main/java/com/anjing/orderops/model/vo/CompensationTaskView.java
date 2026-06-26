package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.CompensationStatus;
import com.anjing.orderops.enums.OrderActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationTaskView {

    private String compensationId;

    private String orderNo;

    private OrderActionType actionType;

    private CompensationStatus status;

    private String failedStep;

    private String compensationAction;

    private int retryCount;

    private String lastError;

    private String createdAt;

    private String updatedAt;
}
