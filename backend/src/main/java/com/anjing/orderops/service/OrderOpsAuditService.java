package com.anjing.orderops.service;

import com.anjing.orderops.enums.AuditEventType;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.model.vo.AuditLogView;
import com.anjing.util.DateUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOpsAuditService {

    private final OrderOpsStore store;

    public AuditLogView record(AuditEventType eventType, String orderNo, OrderActionType actionType,
            String operatorId, String message, String detail) {
        AuditLogView auditLog = AuditLogView.builder()
                .auditId(store.nextAuditId())
                .eventType(eventType)
                .orderNo(orderNo)
                .actionType(actionType)
                .operatorId(operatorId)
                .message(message)
                .detail(detail)
                .createdAt(DateUtils.nowIso())
                .build();
        store.addAuditLog(auditLog);
        return auditLog;
    }

    public List<AuditLogView> list(String orderNo, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return store.listAuditLogs(orderNo, safeLimit);
    }
}
