package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.AuditEventType;
import com.anjing.orderops.enums.OrderActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogView {

    private String auditId;

    private AuditEventType eventType;

    private String orderNo;

    private OrderActionType actionType;

    private String operatorId;

    private String message;

    private String detail;

    private String createdAt;
}
