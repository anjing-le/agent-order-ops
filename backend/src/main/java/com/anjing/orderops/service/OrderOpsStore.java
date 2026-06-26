package com.anjing.orderops.service;

import com.anjing.orderops.enums.ApprovalStatus;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.enums.PaymentStatus;
import com.anjing.orderops.enums.ShipmentStatus;
import com.anjing.orderops.model.domain.ApprovalTicket;
import com.anjing.orderops.model.domain.IdempotencyRecord;
import com.anjing.orderops.model.domain.OrderRecord;
import com.anjing.orderops.model.vo.AuditLogView;
import com.anjing.orderops.model.vo.CompensationTaskView;
import com.anjing.orderops.model.vo.OrderTimelineItem;
import com.anjing.util.DateUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OrderOpsStore {

    private final ConcurrentMap<String, OrderRecord> orders = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ApprovalTicket> approvals = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AuditLogView> auditLogs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompensationTaskView> compensations = new ConcurrentHashMap<>();

    private final AtomicLong planSequence = new AtomicLong();
    private final AtomicLong executionSequence = new AtomicLong();
    private final AtomicLong approvalSequence = new AtomicLong();
    private final AtomicLong auditSequence = new AtomicLong();
    private final AtomicLong compensationSequence = new AtomicLong();

    public OrderOpsStore() {
        seedOrders();
    }

    public List<OrderRecord> listOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(OrderRecord::getOrderNo))
                .toList();
    }

    public Optional<OrderRecord> findOrder(String orderNo) {
        return Optional.ofNullable(orders.get(orderNo));
    }

    public void saveOrder(OrderRecord order) {
        orders.put(order.getOrderNo(), order);
    }

    public Optional<IdempotencyRecord> findIdempotencyRecord(String idempotencyKey) {
        return Optional.ofNullable(idempotencyRecords.get(idempotencyKey));
    }

    public void saveIdempotencyRecord(IdempotencyRecord record) {
        record.setUpdatedAt(DateUtils.nowIso());
        idempotencyRecords.put(record.getIdempotencyKey(), record);
    }

    public void saveApproval(ApprovalTicket approval) {
        approvals.put(approval.getApprovalId(), approval);
    }

    public Optional<ApprovalTicket> findApproval(String approvalId) {
        return Optional.ofNullable(approvals.get(approvalId));
    }

    public List<ApprovalTicket> listApprovals(ApprovalStatus status) {
        return approvals.values().stream()
                .filter(approval -> status == null || approval.getStatus() == status)
                .sorted(Comparator.comparing(ApprovalTicket::getRequestedAt).reversed())
                .toList();
    }

    public void addAuditLog(AuditLogView auditLog) {
        auditLogs.put(auditLog.getAuditId(), auditLog);
    }

    public List<AuditLogView> listAuditLogs(String orderNo, int limit) {
        return auditLogs.values().stream()
                .filter(log -> orderNo == null || orderNo.isBlank() || orderNo.equals(log.getOrderNo()))
                .sorted(Comparator.comparing(AuditLogView::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }

    public void saveCompensation(CompensationTaskView compensationTask) {
        compensationTask.setUpdatedAt(DateUtils.nowIso());
        compensations.put(compensationTask.getCompensationId(), compensationTask);
    }

    public Optional<CompensationTaskView> findCompensation(String compensationId) {
        return Optional.ofNullable(compensations.get(compensationId));
    }

    public Collection<CompensationTaskView> listCompensations() {
        return compensations.values();
    }

    public String nextPlanId() {
        return "PLAN-%06d".formatted(planSequence.incrementAndGet());
    }

    public String nextExecutionId() {
        return "EXEC-%06d".formatted(executionSequence.incrementAndGet());
    }

    public String nextApprovalId() {
        return "APR-%06d".formatted(approvalSequence.incrementAndGet());
    }

    public String nextAuditId() {
        return "AUD-%06d".formatted(auditSequence.incrementAndGet());
    }

    public String nextCompensationId() {
        return "CMP-%06d".formatted(compensationSequence.incrementAndGet());
    }

    private void seedOrders() {
        putOrder("ORD-20260626-1001", "林晓安", "138****4101",
                "上海市浦东新区世纪大道 88 号 18 楼", OrderStatus.PAID, PaymentStatus.PAID,
                ShipmentStatus.NOT_CREATED, "368.00", "368.00", List.of("新客首单"));
        putOrder("ORD-20260626-1002", "赵南星", "186****9920",
                "杭州市西湖区文三路 45 号 3 幢 1202", OrderStatus.FULFILLING, PaymentStatus.PAID,
                ShipmentStatus.ALLOCATED, "1299.00", "1299.00", List.of("仓内拣货", "可拦截"));
        putOrder("ORD-20260626-1003", "陈知予", "177****8365",
                "北京市朝阳区望京东路 9 号 7 层", OrderStatus.SHIPPED, PaymentStatus.PAID,
                ShipmentStatus.IN_TRANSIT, "2599.00", "2599.00", List.of("运输中", "高客诉风险"));
        putOrder("ORD-20260626-1004", "周亦然", "159****6028",
                "深圳市南山区科技园科苑路 66 号", OrderStatus.DELIVERED, PaymentStatus.PAID,
                ShipmentStatus.DELIVERED, "799.00", "799.00", List.of("已签收", "售后窗口内"));
    }

    private void putOrder(String orderNo, String customerName, String phoneMasked, String address,
            OrderStatus orderStatus, PaymentStatus paymentStatus, ShipmentStatus shipmentStatus,
            String paidAmount, String refundableAmount, List<String> riskTags) {
        List<OrderTimelineItem> timeline = new ArrayList<>();
        timeline.add(OrderTimelineItem.builder()
                .time("2026-06-26T02:00:00Z")
                .title("订单创建")
                .detail("订单进入订单运营 Agent 教学沙箱")
                .build());
        timeline.add(OrderTimelineItem.builder()
                .time("2026-06-26T02:05:00Z")
                .title("支付成功")
                .detail("支付金额 " + paidAmount + " 元")
                .build());
        orders.put(orderNo, OrderRecord.builder()
                .orderNo(orderNo)
                .customerName(customerName)
                .phoneMasked(phoneMasked)
                .address(address)
                .orderStatus(orderStatus)
                .paymentStatus(paymentStatus)
                .shipmentStatus(shipmentStatus)
                .paidAmount(new BigDecimal(paidAmount))
                .refundableAmount(new BigDecimal(refundableAmount))
                .refundedAmount(BigDecimal.ZERO)
                .couponIssuedAmount(BigDecimal.ZERO)
                .riskTags(new ArrayList<>(riskTags))
                .timeline(timeline)
                .build());
    }
}
