package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.ExecutionStatus;
import com.anjing.orderops.enums.OrderActionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResultView {

    private String executionId;

    private String idempotencyKey;

    private String orderNo;

    private OrderActionType actionType;

    private ExecutionStatus status;

    private String approvalId;

    private String summary;

    private boolean replay;

    private List<ToolCallView> toolCalls;

    private CompensationTaskView compensationTask;
}
