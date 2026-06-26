package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.ToolCallStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallView {

    private String toolName;

    private String stepName;

    private ToolCallStatus status;

    private String inputSummary;

    private String outputSummary;

    private String startedAt;

    private String finishedAt;
}
